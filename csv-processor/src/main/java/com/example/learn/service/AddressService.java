package com.example.learn.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.learn.dto.ZipCodeResponse;
import com.example.learn.dto.ZippopotamusResponse;
import com.example.learn.entities.Address;
import com.example.learn.repositories.AddressRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;

    // for local zipdb server only
    @Transactional
    public Address getOrCreateAddress(String zipCode, ZipCodeResponse zipCodeResponse) {
        Optional<Address> existingAddress = addressRepository.findByZipCode(zipCode);

        if (existingAddress.isPresent()) {
            return existingAddress.get();
        }

        addressRepository.upsertAddress(zipCode, "US", zipCodeResponse.getAreaName(),
                zipCodeResponse.getPhysicalState());

        return addressRepository.findByZipCode(zipCode)
                .orElseThrow(() -> new IllegalStateException("Address Missing after insert"));

    }

    @Transactional
    public Address getOrCreateAddressFromZip(
            String zipCode,
            ZippopotamusResponse response) {

        Optional<Address> existingAddress = addressRepository.findByZipCode(zipCode);

        if (existingAddress.isPresent()) {
            return existingAddress.get();
        }

        ZippopotamusResponse.Place place = response.getPlaces().get(0);

        addressRepository.upsertAddress(
                zipCode,
                response.getCountryAbbreviation(),
                place.getPlaceName(),
                place.getState());

        return addressRepository.findByZipCode(zipCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Address missing  Zippopotam insert"));
    }
}
