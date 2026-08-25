package com.example.learn.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.learn.entities.CsvJob;

public interface CsvJobRepository  extends JpaRepository<CsvJob,UUID>{
    
}
