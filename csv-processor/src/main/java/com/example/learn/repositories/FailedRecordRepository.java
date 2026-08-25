package com.example.learn.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.learn.entities.FailedRecord;

public interface FailedRecordRepository extends JpaRepository<FailedRecord,Long>{
    
}
