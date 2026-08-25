package com.example.learn.entities;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="failed_record")
@Getter
@Setter
public class FailedRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @ManyToOne
    @JoinColumn(name="job_id",nullable = false)
    private CsvJob job;

    @Column(name="row_number",nullable = false)
    private Integer rowNumber;

    @Column(name="error_message",nullable = false)
    private String errorMessage;
}
