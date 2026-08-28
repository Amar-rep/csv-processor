package com.example.learn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.learn.entities.CsvJob;
import com.example.learn.entities.FailedRecord;
import com.example.learn.repositories.FailedRecordRepository;

@ExtendWith(MockitoExtension.class)
public class FailedRecordTest {
    @Mock
    private FailedRecordRepository failedRecordRepository;

    @Mock
    private CsvJobService csvJobService;

    @InjectMocks
    private FailedRecordService failedRecordService;

    @Test
    void createFailedRecord_shouldSaveRecord() {

        CsvJob job = new CsvJob();

        when(failedRecordRepository.save(any(FailedRecord.class)))
            .thenAnswer(record -> record.getArgument(0));

        FailedRecord result = failedRecordService.createFailedRecord(
                job,
                10,
                "Invalid email");

        assertEquals(job, result.getJob());
        assertEquals(10, result.getRowNumber());

        verify(failedRecordRepository).save(any(FailedRecord.class));
    }
}
