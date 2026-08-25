package com.example.learn.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.learn.entities.CsvJob;
import com.example.learn.entities.StatusTracker;
import com.example.learn.entities.jobStatus;
import com.example.learn.repositories.CsvJobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvJobService {

    private final CsvJobRepository csvJobRepository;

    public CsvJob updateJob(CsvJob job, StatusTracker tracker) {
        job.setCompletedAt(LocalDateTime.now());
        job.setFailedRecords(tracker.getFailedRows().get());
        job.setProcessedRecords(tracker.getProcessedRows().get());
        job.setTotalRecords(tracker.getFailedRows().get() + tracker.getProcessedRows().get());
        if (tracker.getSuccess().get()) {
            job.setStatus(jobStatus.COMPLETED);
        } else {
            job.setStatus(jobStatus.FAILED);
        }
        return csvJobRepository.save(job);
    }

    public CsvJob createJob(MultipartFile file) {
        CsvJob job = new CsvJob();
        job.setFileName(file.getOriginalFilename());
        job.setStatus(jobStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
        return csvJobRepository.save(job);
    }

    public CsvJob updateStatus(UUID jobId, jobStatus status) {
        CsvJob job = csvJobRepository.findById(jobId).orElseThrow();
        job.setStatus(status);
        return csvJobRepository.save(job);
    }

}
