package com.apipietunes.streaming.controller;

import java.util.Locale;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.apipietunes.streaming.config.StreamingProperties;
import com.apipietunes.streaming.service.TrackService;
import com.apipietunes.streaming.util.Range;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StreamingController {
    private final TrackService trackService;
    private final StreamingProperties streamingProperties;
    private final String CACHE_CONTROL_MAX_AGE = "public, max-age=86400";

    @GetMapping("/api/tracks/covers/{id}")
    public ResponseEntity<InputStreamResource> cover(@PathVariable String id) {
        final var cover = trackService.getTrackCoverById(id);

        return ResponseEntity.ok()
                .contentLength(cover.getContentLength())
                .contentType(cover.getContentType())
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_MAX_AGE)
                .body(new InputStreamResource(cover.getContent()));
    }

    @GetMapping("/api/play/{id}.mp3")
    public ResponseEntity<byte[]> play(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeaderValue) {

        Range range = Range.parseHttpRangeString(
                rangeHeaderValue,
                streamingProperties.getDefaultChunkSize(),
                streamingProperties.getInitialChunkSize());

        final var track = trackService.getRangedTrackFileById(id, range);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentLength(track.getContentLength())
                .contentType(track.getContentType())
                .header(HttpHeaders.CONTENT_RANGE, track.getRange())
                .body(track.getContent());
    }
}
