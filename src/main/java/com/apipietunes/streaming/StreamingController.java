package com.apipietunes.streaming;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.apipietunes.streaming.exceptions.ObjectNotFoundException;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StreamingController {
    @Value("${minio.buckets.tracks}")
    public String TRACKS_BUCKET;

    @Value("${minio.buckets.covers}")
    public String COVERS_BUCKET;

    private final MinioClient minioClient;

    @GetMapping("/api/play/{id}.mp3")
    public ResponseEntity<InputStreamResource> play(
            @PathVariable(value = "id") String id,
            @RequestHeader(value = "Range", required = false) String rangeHeaderValue) {

        final var track = getTrackFileById(id);
        final var lengthInBytes = Integer.parseInt(track.headers().get("Content-Length"));

        // Check if the request is for a specific range
        if (isRanged(rangeHeaderValue)) {

            // calculate bytes range
            String[] ranges = rangeHeaderValue.substring(6).split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : lengthInBytes - 1;
            String bytesRange = String.format("bytes %d-%d/%d", start, end, lengthInBytes);

            
            try {
                // skip bytes out of range
                track.skip(start);
            } catch (IOException ignored) {
                log.warn("Error while skiping bytes in ranged request. Track id: '{}'", id);
            }

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header("Content-Range", bytesRange)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(end - start + 1)
                    .body(new InputStreamResource(track));

        }

        // If no range is requested, return the entire file
        return ResponseEntity.ok()
                .contentLength(lengthInBytes)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(track));
    }

    @GetMapping("/api/tracks/covers/{id}")
    public ResponseEntity<InputStreamResource> cover(@PathVariable String id) {
        final var cover = getTrackCoverById(id);
        final var lengthInBytes = Integer.parseInt(cover.headers().get("Content-Length"));
        final var contentType = cover.headers().get("Content-Type");

        return ResponseEntity.ok()
                .contentLength(lengthInBytes)
                .header("Cache-Control", "public, max-age=86400")
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(cover));
    }

    private boolean isRanged(String range) {
        return range != null && range.startsWith("bytes=");
    }

    public GetObjectResponse getTrackCoverById(String id) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(COVERS_BUCKET)
                            .object(id)
                            .build());
        } catch (Exception ex) {
            String msg = String.format("Cover '%s' not found", id);
            log.warn(msg);
            throw new ObjectNotFoundException(msg);
        }
    }

    public GetObjectResponse getTrackFileById(String id) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(TRACKS_BUCKET)
                            .object(id)
                            .build());
        } catch (Exception ex) {
            String msg = String.format("Track '%s' not found", id);
            log.warn(msg);
            throw new ObjectNotFoundException(msg);
        }
    }
}
