package com.apipietunes.streaming.aspects;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import com.apipietunes.streaming.exceptions.ApiPieTunesErrorInfo;
import com.apipietunes.streaming.exceptions.ObjectNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(-2)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ObjectNotFoundException.class)
    public ApiPieTunesErrorInfo handleNotFound(ServerWebExchange exchange, Exception ex) {
        log.warn(ex.getMessage());
        return new ApiPieTunesErrorInfo(HttpStatus.NOT_FOUND.value(),
                exchange.getRequest().getPath().toString(), ex.getMessage());
    }
}