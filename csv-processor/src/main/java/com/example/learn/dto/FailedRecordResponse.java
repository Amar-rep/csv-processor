package com.example.learn.dto;

import com.example.learn.entities.FailedRecord;

public record FailedRecordResponse(
        long id,
        Integer rowNumber,
        String errorMessage) {

    public static FailedRecordResponse from(FailedRecord record) {
        return new FailedRecordResponse(
                record.getId(),
                record.getRowNumber(),
                record.getErrorMessage());
    }
}
