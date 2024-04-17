package com.apipietunes.streaming;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class StreamingApplication {
	@Value("${minio.url}")
	private String minioUrl;

	@Value("${minio.accessKey}")
	private String accessKey;

	@Value("${minio.secretKey}")
	private String secretKey;

	@Value("${minio.buckets.tracks}")
	public String TRACKS_BUCKET;

	@Value("${minio.buckets.covers}")
	public String COVERS_BUCKET;

	@Value("${streaming.default-chunk-size}")
    public Integer defaultChankSize;

	@Value("${streaming.initial-chunk-size}")
    public Integer initialChankSize;

	@Bean
	MinioClient minioClient() throws Exception {
		var client = MinioClient.builder()
				.endpoint(minioUrl)
				.credentials(accessKey, secretKey)
				.build();

		boolean isTracksBucketExist = client.bucketExists(BucketExistsArgs.builder().bucket(TRACKS_BUCKET).build());
		boolean isCoversBucketExist = client.bucketExists(BucketExistsArgs.builder().bucket(COVERS_BUCKET).build());

		if (!isTracksBucketExist) {
			client.makeBucket(MakeBucketArgs.builder().bucket(TRACKS_BUCKET).build());
		}

		if (!isCoversBucketExist) {
			client.makeBucket(MakeBucketArgs.builder().bucket(COVERS_BUCKET).build());
		}

		return client;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void afterInitialization() {
		log.info("\n\n\n\n");
		log.info("-------------------------");
		log.info("COVERS_BUCKET: {}", COVERS_BUCKET);
		log.info("TRACKS_BUCKET: {}", TRACKS_BUCKET);
		log.info("DEFAULT_CHANK_SIZE: {}", defaultChankSize);
		log.info("INITIAL_CHANK_SIZE: {}", initialChankSize);
		log.info("URL: {}", minioUrl);
		log.info("-------------------------\n");
	}

	public static void main(String[] args) {
		SpringApplication.run(StreamingApplication.class, args);
	}
}
