package com.example.learn.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Value("${cache.zipcode.maximum-size}")
    private long maximumSize;

   

    @Bean
    public CacheManager CacheManager(){
        CaffeineCacheManager cacheManager=new CaffeineCacheManager("zipcodes");

        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(maximumSize).recordStats());
        return cacheManager;
    }
}
