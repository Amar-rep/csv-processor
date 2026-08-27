package com.example.learn.entities;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusTracker {
    private final AtomicInteger totalRows = new AtomicInteger(0);
    private final AtomicInteger processedRows = new AtomicInteger(0);
    private final AtomicInteger failedRows = new AtomicInteger(0);
    private final AtomicInteger succesfulRows = new AtomicInteger(0);
    private final AtomicBoolean success = new AtomicBoolean(true);

}
