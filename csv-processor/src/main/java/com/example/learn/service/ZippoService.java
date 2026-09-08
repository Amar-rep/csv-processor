package com.example.learn.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.learn.dto.ZipCodeResponse;

import com.example.learn.exceptions.ZipLookupException;
import com.example.learn.service.interfaces.ZipCodeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZippoService implements ZipCodeService {
    private final WebClient webClient;

    @Cacheable(cacheNames = "zippopotamusZipcodes", key = "#zipcode", sync = true)
    public ZipCodeResponse getZipData(String zipcode) {
        String normalizedZipcode = "0".repeat(5 - zipcode.length()) + zipcode;

        try {
            return webClient.get()
                    .uri("https://api.zippopotam.us/us/{zipcode}", normalizedZipcode)
                    .retrieve()
                    .bodyToMono(ZipCodeResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorMap(
                            TimeoutException.class,
                            exception -> new ZipLookupException(
                                    "Zippopotam.us lookup timed out for " + normalizedZipcode,
                                    exception))
                    .block();
        } catch (WebClientResponseException.NotFound exception) {
            return null;
        } catch (WebClientRequestException | WebClientResponseException exception) {
            throw new ZipLookupException(
                    "Zippopotam.us lookup failed for " + normalizedZipcode,
                    exception);
        }
    }

}
