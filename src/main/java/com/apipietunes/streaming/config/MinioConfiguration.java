package com.apipietunes.streaming.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@AllArgsConstructor
public class MinioConfiguration {
	private final MinioProperties minioProperties;
	private final StreamingProperties streamingProperties;

	@Bean
	MinioClient minioClient() throws Exception {
		var client = MinioClient.builder()
				.endpoint(minioProperties.getUrl())
				.credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
				.build();

		boolean isTracksBucketExist = client
				.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketsTracks()).build());
		boolean isCoversBucketExist = client
				.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketsCovers()).build());

		if (!isTracksBucketExist) {
			client.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketsTracks()).build());
		}

		if (!isCoversBucketExist) {
			client.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketsCovers()).build());
		}

		return client;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void afterInitialization() {
		log.info("\n\n\n\n");
		log.info("-------------------------");
		log.info("TRACKS_BUCKET: {}", minioProperties.getBucketsTracks());
		log.info("COVERS_BUCKET: {}", minioProperties.getBucketsCovers());
		log.info("DEFAULT_CHANK_SIZE: {}", streamingProperties.getDefaultChunkSize());
		log.info("INITIAL_CHANK_SIZE: {}", streamingProperties.getInitialChunkSize());
		log.info("URL: {}", minioProperties.getUrl());
		log.info("-------------------------\n");
	}
}
