package com.example.learn.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.learn.entities.CsvJob;
import com.example.learn.entities.jobStatus;

public record CsvJobResponse(
        UUID jobId,
        String fileName,
        jobStatus status,
        Integer processedRecords,
        Integer successfulRecords,
        Integer failedRecords,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public static CsvJobResponse from(CsvJob job) {
        return new CsvJobResponse(
                job.getId(),
                job.getFileName(),
                job.getStatus(),
                job.getProcessedRecords(),
                job.getSuccessfulRecords(),
                job.getFailedRecords(),
                job.getCreatedAt(),
                job.getCompletedAt());
    }
}
