package com.gft.transport.truck.infrastructure.rest;

import com.gft.transport.truck.domain.exception.TruckNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TruckExceptionHandler {

    @ExceptionHandler(TruckNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleTruckNotFound() {
    }
}
