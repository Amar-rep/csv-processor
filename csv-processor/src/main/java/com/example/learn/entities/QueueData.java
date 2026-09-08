package com.example.learn.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class QueueData {
    String line;
    int rowNumber;

    public String getLine() {
        return this.line;
    }

    public int getRowNumber() {
        return rowNumber;
    }
}