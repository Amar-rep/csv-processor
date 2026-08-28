package com.example.learn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.aspectj.apache.bcel.generic.TABLESWITCH;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.example.learn.entities.CsvJob;
import com.example.learn.entities.StatusTracker;
import com.example.learn.entities.jobStatus;
import com.example.learn.repositories.CsvJobRepository;

@ExtendWith(MockitoExtension.class)
public class CsvJobServiceTest {
    @Mock
    private CsvJobRepository csvJobRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private CsvJobService csvJobService;

    @Test
    void createJob_test() {

        when(multipartFile.getOriginalFilename()).thenReturn("users.csv");
        when(csvJobRepository.save(any(CsvJob.class))).thenAnswer(jobs -> jobs.getArgument(0));

        CsvJob job = csvJobService.createJob(multipartFile);
        assertEquals("users.csv", job.getFileName());
        assertNotNull(job.getCreatedAt());
        assertEquals(job.getSuccessfulRecords(), 0);
        assertEquals(job.getStatus(), jobStatus.PENDING);
        verify(csvJobRepository).save(any(CsvJob.class));
    }

    @Test
    void findByJobId_test() {
        CsvJob job = new CsvJob();
        UUID id = UUID.randomUUID();
        job.setId(id);

        when(csvJobRepository.findById(id)).thenReturn(Optional.of(job));

        CsvJob result = csvJobService.findByJobId(id);
        assertEquals(job, result);
        verify(csvJobRepository).findById(id);
    }

    @Test
    void updateStatus_test() {
        CsvJob job = new CsvJob();
        UUID id = UUID.randomUUID();
        job.setId(id);
        when(csvJobRepository.findById(id))
                .thenReturn(Optional.of(job));
        when(csvJobRepository.save(job)).thenReturn(job);

        CsvJob result = csvJobService.updateStatus(id, jobStatus.FAILED);
        assertEquals(jobStatus.FAILED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(csvJobRepository).save(job);
    }

    @Test
    void updateJob_test() {
        CsvJob job = new CsvJob();
        StatusTracker tracker = new StatusTracker();
        tracker.getTotalRows().set(100);
        tracker.getSuccesfulRows().set(90);
        tracker.getFailedRows().set(10);
        tracker.getSuccess().set(true);
        when(csvJobRepository.save(job)).thenReturn(job);
        CsvJob result = csvJobService.updateJob(job, tracker);

        // success case
        assertEquals(jobStatus.COMPLETED, result.getStatus());
        assertEquals(tracker.getTotalRows().get(), result.getProcessedRecords());

        // failed case
        CsvJob failedJob = new CsvJob();
        tracker.getSuccess().set(false);
        CsvJob failedResult = csvJobService.updateJob(failedJob, tracker);
        assertEquals(failedJob.getStatus(), jobStatus.FAILED);
    }

}
