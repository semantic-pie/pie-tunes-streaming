package com.apipietunes.streaming.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "streaming")
public class StreamingProperties {
    private Integer defaultChunkSize;
    private Integer initialChunkSize;
}
