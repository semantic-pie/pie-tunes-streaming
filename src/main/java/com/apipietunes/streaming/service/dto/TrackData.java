package com.apipietunes.streaming.service.dto;

import org.springframework.http.MediaType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackData {
    private Integer contentLength;
    private MediaType contentType;
    private String range;
    private byte[] content;
}
