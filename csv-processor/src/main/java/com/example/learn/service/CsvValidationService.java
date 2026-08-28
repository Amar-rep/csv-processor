package com.example.learn.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.learn.exceptions.CsvValidationException;
import com.example.learn.exceptions.FileProcessingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CsvValidationService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final EmailValidator emailValidator = EmailValidator.getInstance();
    private final UrlValidator urlValidator = UrlValidator.getInstance();

    public void validateFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();

        log.info("Uploaded File: name={},type={}, size={} mb", fileName, contentType, (double) size / (1024 * 1024));

        if (file.isEmpty()) {
            throw new FileProcessingException("uploaded File is empty");
        }
        if (size > MAX_FILE_SIZE) {
            throw new FileProcessingException("File size exceed 20mb");
        }
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new FileProcessingException("Invalid File type");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String header = reader.readLine();
            if (header == null) {
                throw new FileProcessingException("Invalid header");

            }
            String data[] = header.toLowerCase().split(",");

            if (data.length != 7
                    || !data[0].equals("firstname")
                    || !data[1].equals("lastname")
                    || !data[2].equals("zipcode")
                    || !data[3].equals("phone1")
                    || !data[4].equals("phone2")
                    || !data[5].equals("email")
                    || !data[6].equals("web"))
                throw new FileProcessingException("Invalid csv Format");
        }

    }

    public void validateLine(String line) {
        String[] data = line.split(",");
        if (data.length != 7) {
            throw new CsvValidationException("Invalid col numbers");
        }

        String firstName = data[0].trim();
        String lastName = data[1].trim();
        String zipCode = data[2].trim();
        String phone1 = data[3].trim();
        String phone2 = data[4].trim();
        String email = data[5].trim();
        String url = data[6].trim();

        if (firstName.isBlank()) {
            throw new CsvValidationException("First name is required");
        }
        if (lastName.isBlank()) {
            throw new CsvValidationException("Last name is required");
        }
        if (email.isBlank() || !emailValidator.isValid(email)) {
            throw new CsvValidationException("Invalid Email");
        }
        if (url.isBlank() || !urlValidator.isValid(url)) {
            throw new CsvValidationException("Invalid URL");
        }
        if (!phone1.matches("\\d{3}-\\d{3}-\\d{4}") || !phone2.matches("\\d{3}-\\d{3}-\\d{4}")) {
            throw new CsvValidationException("Invalid phone ");
        }
        if (!zipCode.matches("\\d{4,5}")) {
            throw new CsvValidationException("Invalid ZIP code");
        }
    }
}
