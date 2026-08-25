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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.learn.dto.ZipCodeResponse;
import com.example.learn.entities.CsvJob;
import com.example.learn.entities.StatusTracker;
import com.example.learn.entities.jobStatus;
import com.example.learn.exceptions.CsvValidationException;
import com.example.learn.exceptions.FileProcessingException;
import com.example.learn.repositories.CsvJobRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class csvProcessingService {

    private final UserProcessingService userProcessingService;
    private final ZipcodeService zipcodeService;
    private final CsvJobService csvJobService;

    private final EmailValidator Email_Validator = EmailValidator.getInstance();
    private final UrlValidator Url_Validator = UrlValidator.getInstance();

    private final ExecutorService consumerPool = Executors.newFixedThreadPool(CONSUMER_COUNT);
    private final ExecutorService producerPool = Executors.newFixedThreadPool(3);
    private final ExecutorService jobPool = Executors.newFixedThreadPool(3);
    private static final int CONSUMER_COUNT = 10;

    private final String POISON = "__DONE__";

    // start job
    public UUID launchCsvProcessing(MultipartFile file) {

        fileValidation(file);
        CsvJob job = csvJobService.createJob(file);
        UUID jobId = job.getId();
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("csv-" + jobId + "--", ".csv");
            file.transferTo(tempFile);
            Path fileForProcesssing = tempFile;
            // send for processing
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
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(100, false);
        List<Future<?>> consumerFutures = new ArrayList<>();
        csvJobService.updateStatus(job.getId(), jobStatus.PROCESSING);

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            Future<?> future = consumerPool.submit(() -> consumer(queue, tracker, job));
            consumerFutures.add(future);
        }
        Future<?> producerFuture = producerPool.submit(() -> produce(job, tempFile, queue, tracker, consumerFutures));

        // Ending the process
        try {
            producerFuture.get();
            for (Future<?> ft : consumerFutures) {
                ft.get();
            }
            System.out.println(LocalDateTime.now());
            csvJobService.updateJob(job, tracker);
            deleteTempFile(tempFile);

        } catch (Exception e) {
            log.error("CSV processsing failed for job {}", job.getId());
            csvJobService.updateStatus(job.getId(), jobStatus.FAILED);
        } finally {
            deleteTempFile(tempFile);
        }

    }

    private void produce(CsvJob job, Path tempFile, BlockingQueue<String> queue, StatusTracker tracker,
            List<Future<?>> consumerFutures) {
        int rowNumber = 1;
        try (BufferedReader reader = Files.newBufferedReader(tempFile)) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                tracker.getTotalRows().incrementAndGet();
                rowNumber++;
                validateAndQueue(job, line, rowNumber, queue, tracker);
            }
            sendPoisonPill(queue);
        } catch (IOException e) {
            handleProducerFailure(job, tracker, consumerFutures, "failed to read csv file", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleProducerFailure(job, tracker, consumerFutures, "Producer Interrupted", e);
        }
    }

    private void consumer(BlockingQueue<String> queue, StatusTracker tracker, CsvJob job) {
        while (true) {
            try {
                String line = queue.take();

                if (POISON.equals(line)) {
                    break;
                }

                String userData[] = line.split(",");

                String zipcode = userData[2];
                List<ZipCodeResponse> responses = zipcodeService.getZipData(zipcode);
                if (responses == null || responses.isEmpty()) {
                    tracker.getFailedRows().incrementAndGet();
                    log.warn("ZIP data not found for: {}", zipcode);
                    continue;
                }

                userProcessingService.createUser(userData, responses.get(0), job);
                tracker.getProcessedRows().incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                tracker.getFailedRows().incrementAndGet();
                log.error("Consumer failed for job {}", job.getId(), e);
            }
        }
    }

    // ----------- Validation Helpers

    private void validateAndQueue(CsvJob job, String line, int rowNumber, BlockingQueue<String> queue,
            StatusTracker tracker) throws InterruptedException {

        try {
            validateLine(line);
            queue.put(line);
        } catch (CsvValidationException e) {
            log.error("Invalid LINE jobId:{} RowNum:{} Message:{}", job.getId(), rowNumber, e.getMessage());
            tracker.getFailedRows().incrementAndGet();
            // store to failed records
        }
    }

    private void fileValidation(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();

        log.info("Uploaded File: name={},type={}, size={} mb", fileName, contentType, (double) size / (1024 * 1024));

        if (file.isEmpty()) {
            throw new FileProcessingException("uploaded File is empty");
        }
        if (size > 20L * 1024 * 1024) {
            throw new FileProcessingException("File size exceed 20mb");

        }
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new FileProcessingException("Invalid File type");
        }

    }

    private void validateLine(String line) {
        String[] data = line.split(",");
        if (data.length != 7) {
            throw new CsvValidationException("Invalid col numbers");
        }
        String email = data[5].trim();
        String firstName = data[0].trim();
        String lastName = data[1].trim();
        String zipCode = data[2].trim();
        String phone1 = data[3].trim();
        String phone2 = data[4].trim();
        String url = data[6].trim();
        if (firstName.isBlank()) {
            throw new CsvValidationException("First name is required");
        }
        if (lastName.isBlank()) {
            throw new CsvValidationException("Last name is required");
        }
        if (email == null || email.isBlank() || !Email_Validator.isValid(email)) {
            throw new CsvValidationException("Invalid Email");
        }
        if (url.isBlank() || !Url_Validator.isValid(url)) {
            throw new CsvValidationException("Invalid URL");
        }
        if (!phone1.matches("\\d{3}-\\d{3}-\\d{4}") || !phone2.matches("\\d{3}-\\d{3}-\\d{4}")) {
            throw new CsvValidationException("Invalid phone ");
        }
        /*
         * if (!zipCode.matches("\\d{5}")) {
         * throw new CsvValidationException("Invalid zipcode ");
         * }
         */

    }

    // ------- CLEAN UP HELPERS

    private void sendPoisonPill(BlockingQueue<String> queue) {
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            try {
                queue.put(POISON);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while signaling CSV consumers to stop", e);
                return;
            }

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
        }
    }

    private void handleProducerFailure(CsvJob job, StatusTracker tracker, List<Future<?>> consumFutures, String message,
            Exception e) {
        log.error("CsvProducer Failure : {} for job {}", message, job.getId());
        tracker.getSuccess().set(false);
        for (Future<?> ft : consumFutures) {
            ft.cancel(true);
        }

    }

    private class queueData {
        String line;
        int rowNumber;
    }

}
