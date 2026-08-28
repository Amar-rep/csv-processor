package com.example.learn.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.example.learn.exceptions.CsvValidationException;
import com.example.learn.exceptions.FileProcessingException;

@ExtendWith(MockitoExtension.class)
public class CsvValidationTest {

    @InjectMocks
    private CsvValidationService csvValidationService;

    @Test
    void accept_validCsvFile() {
        String csvData = "firstName,lastName,zipcode,phone1,phone2,email,web";

        MockMultipartFile file = new MockMultipartFile("file", "user.csv", "text/csv", csvData.getBytes());
        assertDoesNotThrow(() -> csvValidationService.validateFile(file));

    }

    @Test
    void reject_InvalidCsvFile() {
        String csvData = "firstName,lastName,zipcode,phone1,email,web";

        MockMultipartFile file = new MockMultipartFile("file", "user.csv", "text/csv", csvData.getBytes());
        assertThrows(FileProcessingException.class, () -> csvValidationService.validateFile(file));

    }

    @Test
    void accept_ValidLine() {

        String line = "Rodri,Barca,12345,123-456-7890,987-654-3210,Barca@test.com,https://Barca.com";

        assertDoesNotThrow(
                () -> csvValidationService.validateLine(line));
    }

    @Test
    void reject_InvalidLine() {

        String line = "Lamine,Yamal,12345,123-456-7890,987-654-3210,Lamine@test.com,htt://RealMadrid.com";

        assertThrows(CsvValidationException.class,
                () -> csvValidationService.validateLine(line));
    }

}
