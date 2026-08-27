package com.example.learn.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.learn.dto.FailedRecordResponse;
import com.example.learn.entities.CsvJob;
import com.example.learn.entities.FailedRecord;
import com.example.learn.repositories.FailedRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FailedRecordService {
    private final FailedRecordRepository failedRecordRepository;
    private final CsvJobService csvJobService;

    public FailedRecord createFailedRecord(CsvJob job, int rowNumber, String message) {
        FailedRecord record = new FailedRecord();
        record.setJob(job);
        record.setRowNumber(rowNumber);
        record.setErrorMessage(message);
        return failedRecordRepository.save(record);
    }

    public List<FailedRecordResponse> findByJobId(UUID jobId) {
        csvJobService.findByJobId(jobId);
        return failedRecordRepository.findByJobIdOrderByRowNumber(jobId).stream().map(FailedRecordResponse::from)
                .toList();
    }
}
