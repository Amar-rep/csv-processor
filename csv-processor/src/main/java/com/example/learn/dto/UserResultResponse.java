package com.example.learn.dto;

import com.example.learn.entities.Address;
import com.example.learn.entities.Users;

public record UserResultResponse(
        Long id,
        String firstName,
        String lastName,
        String phoneNumber1,
        String phoneNumber2,
        String email,
        String websiteUrl,
        AddressResponse address) {

    public static UserResultResponse from(Users user) {
        return new UserResultResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber1(),
                user.getPhoneNumber2(),
                user.getEmail(),
                user.getWebsiteUrl(),
                AddressResponse.from(user.getAddress()));
    }

    public record AddressResponse(
            String zipCode,
            String city,
            String state,
            String country) {

        private static AddressResponse from(Address address) {
            if (address == null) {
                return null;
            }
            return new AddressResponse(
                    address.getZipCode(),
                    address.getCity(),
                    address.getState(),
                    address.getCountry());
        }
    }
}
