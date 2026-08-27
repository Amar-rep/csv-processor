package com.example.learn.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZippopotamusResponse {

    @JsonProperty("post code")
    private String postCode;

    private String country;

    @JsonProperty("country abbreviation")
    private String countryAbbreviation;

    private List<Place> places;

    @Getter
    @Setter
    public static class Place {

        @JsonProperty("place name")
        private String placeName;

        private String longitude;
        private String state;

        @JsonProperty("state abbreviation")
        private String stateAbbreviation;

        private String latitude;
    }
}
