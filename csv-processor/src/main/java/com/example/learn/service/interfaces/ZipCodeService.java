package com.example.learn.service.interfaces;

import com.example.learn.dto.ZipCodeResponse;

public interface ZipCodeService {
    ZipCodeResponse getZipData(String zipcode);
}
