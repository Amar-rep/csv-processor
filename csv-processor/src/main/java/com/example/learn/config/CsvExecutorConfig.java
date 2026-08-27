package com.example.learn.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CsvExecutorConfig {

    @Value("${csv.executor.consumers}")
    private int consumers;

    @Value("${csv.executor.jobPool}")
    private int jobPool;

    @Value("${csv.executor.producers}")
    private int producers;

    @Bean(name = "csvConsumerExecutor", destroyMethod = "shutdown")
    public ExecutorService csvConsumerExecutor() {
        return Executors.newFixedThreadPool(consumers);
    }

    @Bean(name = "csvProducerExecutor", destroyMethod = "shutdown")
    public ExecutorService csvProducerExecutor() {
        return Executors.newFixedThreadPool(producers);
    }

    @Bean(name = "csvJobExecutor", destroyMethod = "shutdown")
    public ExecutorService csvJobExecutor() {
        return Executors.newFixedThreadPool(jobPool);
    }
}
