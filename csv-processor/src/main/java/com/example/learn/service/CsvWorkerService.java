package com.example.learn.service;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.example.learn.dto.ZippopotamusResponse;
import com.example.learn.entities.CsvJob;
import com.example.learn.entities.QueueData;
import com.example.learn.entities.StatusTracker;
import com.example.learn.exceptions.CsvValidationException;
import com.example.learn.exceptions.FileProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvWorkerService {

    private final ZipcodeService zipcodeService;
    private final CsvValidationService csvValidationService;
    private final UserProcessingService userProcessingService;
    private final FailedRecordService failedRecordService;

    @Value("${csv.executor.consumers}")
    private int CONSUMER_COUNT;

    public void producer(CsvJob job, Path tempFile, BlockingQueue<QueueData> queue, StatusTracker tracker,

            List<Future<?>> consumerFutures, QueueData POISON) {
        int rowNumber = 1;
        try (BufferedReader reader = Files.newBufferedReader(tempFile)) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                tracker.getTotalRows().incrementAndGet();
                rowNumber++;
                queue.put(new QueueData(line, rowNumber));
                tracker.getProcessedRows().incrementAndGet();
            }

            sendPoisonPill(queue, POISON);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw handleProducerFailure(job, tracker, consumerFutures, "Producer Interrupted", e);
        } catch (IOException e) {
            throw handleProducerFailure(job, tracker, consumerFutures, "failed to read csv ", e);
        }
    }

    public void consumer(BlockingQueue<QueueData> queue, StatusTracker tracker, CsvJob job, QueueData POISON) {
        while (true) {
            QueueData data = null;
            try {
                data = queue.take();

                if (POISON == data) {
                    break;
                }

                csvValidationService.validateLine(data.getLine());

                String userData[] = data.getLine().split(",");

                String zipcode = userData[2].trim();

                ZippopotamusResponse response = zipcodeService.getZippopotamusZipData(zipcode);
                if (response == null || response.getPlaces() == null || response.getPlaces().isEmpty()) {
                    tracker.getFailedRows().incrementAndGet();
                    log.warn("ZIP data not found jobId:{} RowNum:{} ZIP:{}", job.getId(), data.getRowNumber(), zipcode);
                    continue;
                }

                userProcessingService.createUserFromZippopotamus(userData, response, job);
                tracker.getSuccesfulRows().incrementAndGet();
            } catch (CsvValidationException e) {
                log.warn("Invalid LINE jobId:{} RowNum:{} Message:{}", job.getId(), data.getRowNumber(),
                        e.getMessage());
                tracker.getFailedRows().incrementAndGet();
                failedRecordService.createFailedRecord(job, data.getRowNumber(), e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Consumer shut oof" + job.getId());
            }
        }
    }

    // thread kill function
    private void sendPoisonPill(BlockingQueue<QueueData> queue, QueueData POISON) throws InterruptedException {
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            queue.put(POISON);
        }
    }

    // handle failures
    private FileProcessingException handleProducerFailure(CsvJob job, StatusTracker tracker,
            List<Future<?>> consumerFutures,
            String message,
            Exception e) {
        log.error("CsvProducer Failure : {} for job {}", message, job.getId());
        tracker.getSuccess().set(false);
        for (Future<?> ft : consumerFutures) {
            ft.cancel(true);
        }
        return new FileProcessingException(message, e);

    }

}
