package com.apipietunes.streaming.service.dto;

import java.io.InputStream;

import org.springframework.http.MediaType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoverData {
    private Integer contentLength;
    private MediaType contentType;
    private InputStream content;
}
