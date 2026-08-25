package com.example.learn.exceptions;

public class CsvValidationException extends RuntimeException {
    public CsvValidationException(String message) {
        super(message);
    }
}
