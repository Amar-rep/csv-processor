package com.example.learn.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.learn.entities.CsvJob;
import com.example.learn.entities.StatusTracker;
import com.example.learn.entities.jobStatus;

import com.example.learn.exceptions.FileProcessingException;
import com.example.learn.entities.QueueData;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class csvProcessingService {

    private final CsvJobService csvJobService;
    private final CsvValidationService csvValidationService;
    private final CsvWorkerService csvWorkerService;

    private final ExecutorService consumerPool;
    private final ExecutorService producerPool;
    private final ExecutorService jobPool;

    @Value("${csv.executor.consumers}")
    private int CONSUMER_COUNT;

    public csvProcessingService(
            UserProcessingService userProcessingService,

            CsvJobService csvJobService,
            FailedRecordService failedRecordService,
            CsvValidationService csvValidationService,
            CsvWorkerService csvWorkerService,

            @Qualifier("csvConsumerExecutor") ExecutorService consumerPool,

            @Qualifier("csvProducerExecutor") ExecutorService producerPool,

            @Qualifier("csvJobExecutor") ExecutorService jobPool) {

        this.csvJobService = csvJobService;
        this.csvValidationService = csvValidationService;
        this.consumerPool = consumerPool;
        this.producerPool = producerPool;
        this.jobPool = jobPool;
        this.csvWorkerService = csvWorkerService;
    }

    private static final QueueData POISON = new QueueData(null, -1);

    // start job
    public UUID launchCsvProcessing(MultipartFile file) {
        Path tempFile = null;
        UUID jobId = null;
        // test
        try {
            csvValidationService.validateFile(file);
            CsvJob job = csvJobService.createJob(file);
            jobId = job.getId();

            tempFile = Files.createTempFile("csv-" + jobId + "--", ".csv");
            file.transferTo(tempFile);

            Path fileForProcesssing = tempFile;
            // send for processing to Jobpool
            jobPool.submit(() -> processCsv(fileForProcesssing, job));
        } catch (IOException | FileProcessingException e) {
            deleteTempFile(tempFile);

            if (jobId != null) {
                csvJobService.updateStatus(jobId, jobStatus.FAILED);
            }

            throw new FileProcessingException("Failed to process the uploaded CSV file", e);
        }

        return jobId;
    }

    public void processCsv(Path tempFile, CsvJob job) {

        StatusTracker tracker = new StatusTracker();
        BlockingQueue<QueueData> queue = new ArrayBlockingQueue<>(100, false);
        List<Future<?>> consumerFutures = new ArrayList<>();
        csvJobService.updateStatus(job.getId(), jobStatus.PROCESSING);

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            Future<?> future = consumerPool.submit(() -> csvWorkerService.consumer(queue, tracker, job, POISON));
            consumerFutures.add(future);
        }
        Future<?> producerFuture = producerPool
                .submit(() -> csvWorkerService.producer(job, tempFile, queue, tracker, consumerFutures, POISON));

        // Ending the process+cleanUP
        try {

            for (Future<?> ft : consumerFutures) {
                ft.get();
            }
            producerFuture.get();

            System.out.println(LocalDateTime.now());
            csvJobService.updateJob(job, tracker);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            cancelTasks(producerFuture, consumerFutures, tracker);

            log.error(
                    " processing interrupted for job {}",
                    job.getId(),
                    e);

            csvJobService.updateJob(job, tracker);

        } catch (ExecutionException e) {
            cancelTasks(producerFuture, consumerFutures, tracker);

            Throwable cause = e.getCause() != null
                    ? e.getCause()
                    : e;

            log.error(
                    "CSV processing failed fo r job {}",
                    job.getId(),
                    cause.getMessage());

            csvJobService.updateJob(job, tracker);

        } finally {
            deleteTempFile(tempFile);
        }

    }

    // clean up functions

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException exception) {
            // write logging
            log.warn("Failed to delete temp file: {}", tempFile, exception);
        }
    }

    private void cancelTasks(
            Future<?> producerFuture,
            List<Future<?>> consumerFutures, StatusTracker tracker) {

        if (!producerFuture.isDone()) {
            producerFuture.cancel(true);
        }

        for (Future<?> future : consumerFutures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        tracker.getSuccess().set(false);
    }

}
