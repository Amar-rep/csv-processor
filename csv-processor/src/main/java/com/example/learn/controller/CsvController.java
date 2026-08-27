package com.example.learn.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.learn.dto.CsvJobResponse;
import com.example.learn.dto.FailedRecordResponse;
import com.example.learn.dto.UserResultResponse;
import com.example.learn.service.CsvJobService;
import com.example.learn.service.FailedRecordService;
import com.example.learn.service.UserProcessingService;
import com.example.learn.service.csvProcessingService;

import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class CsvController {

    private final csvProcessingService csvProcessingService;
    private final CsvJobService csvJobService;
    private final UserProcessingService userProcessingService;
    private final FailedRecordService failedRecordService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> createJob(
            @RequestParam("file") MultipartFile file) {
        UUID jobId = csvProcessingService.launchCsvProcessing(file);
        return ResponseEntity.accepted()
                .body(jobId);
    }

    @GetMapping("/{jobId}")
    public CsvJobResponse getJob(@PathVariable UUID jobId) {
        return CsvJobResponse.from(csvJobService.findByJobId(jobId));
    }

    @GetMapping("/{jobId}/results")
    public ResponseEntity<List<UserResultResponse>> getResults(@PathVariable UUID jobId) {

        return ResponseEntity.ok(userProcessingService.findUsersByJobId(jobId));
    }

    @GetMapping("/{jobId}/failures")
    public ResponseEntity<List<FailedRecordResponse>> getFailures(@PathVariable UUID jobId) {

        return ResponseEntity.ok(failedRecordService.findByJobId(jobId));
    }

}
