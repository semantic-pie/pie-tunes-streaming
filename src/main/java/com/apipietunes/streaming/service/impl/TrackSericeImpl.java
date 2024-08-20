package com.apipietunes.streaming.service.impl;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.apipietunes.streaming.config.MinioProperties;
import com.apipietunes.streaming.exception.ObjectNotFoundException;
import com.apipietunes.streaming.service.TrackService;
import com.apipietunes.streaming.service.dto.CoverData;
import com.apipietunes.streaming.service.dto.TrackData;
import com.apipietunes.streaming.util.Range;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TrackSericeImpl implements TrackService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public CoverData getTrackCoverById(String id) {
        final var coverResponseObject = minioGetTrackCoverById(id);

        final var contentType = MediaType.parseMediaType(coverResponseObject.headers().get(HttpHeaders.CONTENT_TYPE));
        final var lengthInBytes = Integer.parseInt(coverResponseObject.headers().get(HttpHeaders.CONTENT_LENGTH));

        return CoverData.builder()
                .content(coverResponseObject)
                .contentLength(lengthInBytes)
                .contentType(contentType)
                .build();
    }

    @Override
    public TrackData getRangedTrackFileById(String id, Range range) {
        final var trackFileStat = minioGetTrackFileStatById(id);

        final var chunkBytes = readChunk(id, range, trackFileStat.size());
        final var contentType = MediaType.parseMediaType(trackFileStat.headers().get(HttpHeaders.CONTENT_TYPE));
        final var lengthInBytes = Integer.parseInt(trackFileStat.headers().get(HttpHeaders.CONTENT_LENGTH));

        return TrackData.builder()
                .content(chunkBytes)
                .contentLength(lengthInBytes)
                .contentType(contentType)
                .range(Range.constructContentRangeHeader(range, trackFileStat.size()))
                .build();
    }

    private byte[] readChunk(String id, Range range, long fileSize) {
        // Reads a specific byte range (chunk) from the track file and
        // then returns the chunk as a byte array.

        long startPosition = range.getRangeStart();
        long endPosition = range.getRangeEnd(fileSize);
        int chunkSize = (int) (endPosition - startPosition + 1);
        try (var inputStream = minioGetTrackFileById(id, startPosition, chunkSize)) {
            return inputStream.readAllBytes();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public GetObjectResponse minioGetTrackCoverById(String id) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketsCovers())
                            .object(id)
                            .build());
        } catch (Exception ex) {
            String msg = String.format("Cover '%s' not found", id);
            throw new ObjectNotFoundException(msg);
        }
    }

    public StatObjectResponse minioGetTrackFileStatById(String id) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketsTracks())
                    .object(id)
                    .build());
        } catch (Exception ex) {
            String msg = String.format("Track '%s' not found", id);
            throw new ObjectNotFoundException(msg);
        }
    }

    public GetObjectResponse minioGetTrackFileById(String id, long offset, long length) {
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
            throw new ObjectNotFoundException(msg);
        }
    }
}
