package com.example.learn.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.learn.dto.ZipCodeResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZipcodeService {
    private final WebClient webClient;

    @Cacheable(cacheNames = "zipcodes",key="#zipcode",sync = true)
    public List<ZipCodeResponse> getZipData(String zipcode){
        return webClient.get().uri("http://localhost:8080/api/zipcodes/delivery/{zipcode}",zipcode)
                .retrieve()
                .bodyToFlux(ZipCodeResponse.class).collectList().block();
    }

}
