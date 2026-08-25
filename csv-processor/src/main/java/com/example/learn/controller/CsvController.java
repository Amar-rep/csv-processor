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

import com.example.learn.dto.ZipCodeResponse;
import com.example.learn.entities.Users;
import com.example.learn.repositories.UserRepository;
import com.example.learn.service.ZipcodeService;
import com.example.learn.service.csvProcessingService;

import lombok.RequiredArgsConstructor;




@RestController
@RequestMapping("/csv")
@RequiredArgsConstructor

public class CsvController {
    public final csvProcessingService csvProcessingService;
    public final UserRepository userRepository;
    private final ZipcodeService zipcodeService;
    @PostMapping("api/jobs")
    public UUID uploadCsvFile(@RequestParam("file") MultipartFile file) {
        
        return csvProcessingService.launchCsvProcessing(file);
    }

    @GetMapping("api/jobs/{jobId}")
    public ResponseEntity<List<Users>> getMethodName(@PathVariable UUID jobId) {
        return ResponseEntity.ok(userRepository.findByJobId(jobId));
    }

    @GetMapping("api/zipData/{zipcode}")
    public ResponseEntity<ZipCodeResponse> getZipData(@PathVariable String zipcode) {
        return ResponseEntity.ok(zipcodeService.getZipData(zipcode).get(0));
    }
    

    
}
