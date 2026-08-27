package com.example.learn.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.learn.entities.FailedRecord;

public interface FailedRecordRepository extends JpaRepository<FailedRecord,Long>{
    List<FailedRecord> findByJobIdOrderByRowNumber(UUID jobId);
}
