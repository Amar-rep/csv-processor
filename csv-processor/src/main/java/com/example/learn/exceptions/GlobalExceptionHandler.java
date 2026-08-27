package com.example.learn.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(FileProcessingException.class)
        public ResponseEntity<ErrorResponse> handleFileProcessingException(
                        FileProcessingException exception,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(

                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                exception.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        public record ErrorResponse(
                        int status,
                        String message,
                        String path) {
        }
}
