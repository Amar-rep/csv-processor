package com.example.learn.service;

import java.io.BufferedReader;
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

import com.example.learn.dto.ZippopotamusResponse;
import com.example.learn.entities.CsvJob;
import com.example.learn.entities.StatusTracker;
import com.example.learn.entities.jobStatus;
import com.example.learn.exceptions.CsvValidationException;
import com.example.learn.exceptions.FileProcessingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class csvProcessingService {

    private final UserProcessingService userProcessingService;
    private final ZipcodeService zipcodeService;
    private final CsvJobService csvJobService;
    private final FailedRecordService failedRecordService;
    private final CsvValidationService csvValidationService;

    private final ExecutorService consumerPool;
    private final ExecutorService producerPool;
    private final ExecutorService jobPool;

    @Value("${csv.executor.consumers}")
    private int CONSUMER_COUNT;

    public csvProcessingService(
            UserProcessingService userProcessingService,
            ZipcodeService zipcodeService,
            CsvJobService csvJobService,
            FailedRecordService failedRecordService,
            CsvValidationService csvValidationService,

            @Qualifier("csvConsumerExecutor") ExecutorService consumerPool,

            @Qualifier("csvProducerExecutor") ExecutorService producerPool,

            @Qualifier("csvJobExecutor") ExecutorService jobPool) {

        this.userProcessingService = userProcessingService;
        this.zipcodeService = zipcodeService;
        this.csvJobService = csvJobService;
        this.failedRecordService = failedRecordService;
        this.csvValidationService = csvValidationService;

        this.consumerPool = consumerPool;
        this.producerPool = producerPool;
        this.jobPool = jobPool;
    }

    private static final QueueData POISON = new QueueData(null, -1);

    // start job
    public UUID launchCsvProcessing(MultipartFile file) {
        Path tempFile = null;
        UUID jobId = null;
        try {
            csvValidationService.validateFile(file);
            CsvJob job = csvJobService.createJob(file);
            jobId = job.getId();

            tempFile = Files.createTempFile("csv-" + jobId + "--", ".csv");
            file.transferTo(tempFile);

            Path fileForProcesssing = tempFile;
            // send for processing to Jobpool
            jobPool.submit(() -> processCsv(fileForProcesssing, job));
        } catch (IOException e) {
            deleteTempFile(tempFile);
            csvJobService.updateStatus(jobId, jobStatus.FAILED);
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
            Future<?> future = consumerPool.submit(() -> consumer(queue, tracker, job));
            consumerFutures.add(future);
        }
        Future<?> producerFuture = producerPool.submit(() -> produce(job, tempFile, queue, tracker, consumerFutures));

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

    private void produce(CsvJob job, Path tempFile, BlockingQueue<QueueData> queue, StatusTracker tracker,
            List<Future<?>> consumerFutures) {
        int rowNumber = 1;
        try (BufferedReader reader = Files.newBufferedReader(tempFile)) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                tracker.getTotalRows().incrementAndGet();
                rowNumber++;
                queue.put(new QueueData(line, rowNumber));
                tracker.getProcessedRows().incrementAndGet();
            }

            sendPoisonPill(queue);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw handleProducerFailure(job, tracker, consumerFutures, "Producer Interrupted", e);
        } catch (IOException e) {
            throw handleProducerFailure(job, tracker, consumerFutures, "failed to read csv ", e);
        }
    }

    private void consumer(BlockingQueue<QueueData> queue, StatusTracker tracker, CsvJob job) {
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
                failedRecordService.createFailedRecord(job, data.rowNumber, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // ------- CLEAN UP HELPERS

    private void sendPoisonPill(BlockingQueue<QueueData> queue) throws InterruptedException {
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            queue.put(POISON);
        }
    }

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

    private static class QueueData {
        String line;
        int rowNumber;

        QueueData(String line, int rowNumber) {
            this.line = line;
            this.rowNumber = rowNumber;
        }

        public String getLine() {
            return this.line;
        }

        public int getRowNumber() {
            return rowNumber;
        }
    }

}
