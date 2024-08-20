package com.apipietunes.streaming.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.apipietunes.streaming.config.MinioProperties;
import com.apipietunes.streaming.config.StreamingProperties;
import com.apipietunes.streaming.exception.ObjectNotFoundException;
import com.apipietunes.streaming.util.Range;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StreamingController {
    private final StreamingProperties streamingProperties;
    private final MinioProperties minioProperties;
    private final MinioClient minioClient;
    private final String CACHE_CONTROL_MAX_AGE = "public, max-age=86400";

    @GetMapping("/api/tracks/covers/{id}")
    public ResponseEntity<InputStreamResource> cover(@PathVariable String id) {
        final var cover = getTrackCoverById(id);
        final var lengthInBytes = Integer.parseInt(cover.headers().get(HttpHeaders.CONTENT_LENGTH));
        final var contentType = cover.headers().get(HttpHeaders.CONTENT_TYPE);

        return ResponseEntity.ok()
                .contentLength(lengthInBytes)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_MAX_AGE)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(cover));
    }

    @GetMapping("/api/play/{id}.mp3")
    public ResponseEntity<byte[]> play(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeaderValue) {

        Range range = Range.parseHttpRangeString(
                rangeHeaderValue,
                streamingProperties.getDefaultChunkSize(),
                streamingProperties.getInitialChunkSize());

        var stat = getTrackFileStatById(id);
        var chunk = readChunk(id, range, stat.size());

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, stat.contentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(stat.size()))
                .header(HttpHeaders.CONTENT_RANGE, constructContentRangeHeader(range, stat.size()))
                .body(chunk);
    }

    private String constructContentRangeHeader(Range range, long fileSize) {
        return "bytes " + range.getRangeStart() + "-" + range.getRangeEnd(fileSize) + "/" + fileSize;
    }

    private byte[] readChunk(String id, Range range, long fileSize) {
        long startPosition = range.getRangeStart();
        long endPosition = range.getRangeEnd(fileSize);
        int chunkSize = (int) (endPosition - startPosition + 1);
        try (var inputStream = getTrackFileById(id, startPosition, chunkSize)) {
            return inputStream.readAllBytes();
        } catch (Exception exception) {
            log.error("Exception occurred when trying to read file with ID = {}", id);
            throw new RuntimeException(exception);
        }
    }

    public GetObjectResponse getTrackCoverById(String id) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketsCovers())
                            .object(id)
                            .build());
        } catch (Exception ex) {
            String msg = String.format("Cover '%s' not found", id);
            log.warn(msg);
            throw new ObjectNotFoundException(msg);
        }
    }

    public StatObjectResponse getTrackFileStatById(String id) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketsTracks())
                    .object(id)
                    .build());
        } catch (Exception ex) {
            String msg = String.format("Cover '%s' not found", id);
            log.warn(msg);
            throw new ObjectNotFoundException(msg);
        }
    }

    public GetObjectResponse getTrackFileById(String id, long offset, long length) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketsTracks())
                            .object(id)
                            .offset(offset)
                            .length(length)
                            .build());
        } catch (Exception ex) {
            String msg = String.format("Track '%s' not found", id);
            log.warn(msg);
            throw new ObjectNotFoundException(msg);
        }
    }

    public GetObjectResponse getTrackFileById(String id) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketsTracks())
                            .object(id)
                            .build());
        } catch (Exception ex) {
            String msg = String.format("Track '%s' not found", id);
            log.warn(msg);
            throw new ObjectNotFoundException(msg);
        }
    }
}
