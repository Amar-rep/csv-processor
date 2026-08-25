package com.example.learn.service;

import java.util.Optional;

   
import org.springframework.stereotype.Service;

import com.example.learn.dto.ZipCodeResponse;
import com.example.learn.entities.Address;
import com.example.learn.repositories.AddressRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;

    @Transactional
    public Address getOrCreateAddress(String zipCode,ZipCodeResponse zipCodeResponse){
        Optional<Address> existingAddress=addressRepository.findByZipCode(zipCode);

        if(existingAddress.isPresent()){
            return existingAddress.get();
        }

        Address address=new Address();
        address.setZipCode(zipCode);
        address.setCountry("US");
        address.setCity(zipCodeResponse.getAreaName());
        address.setState(zipCodeResponse.getPhysicalState());

       
        return addressRepository.save(address);
      
    }
}
