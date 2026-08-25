package com.example.learn.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZipCodeResponse {
    private Long id;
    private String areaName;
    private String areaCode;
    private String districtName;
    private String districtNo;
    private String deliveryZipcode;
    private String localName;
    private String physicalDelivAddr;
    private String physicalCity;
    private String physicalState;
    private String physicalZip;
    private String physicalZip4;
}
