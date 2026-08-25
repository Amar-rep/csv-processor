package com.example.learn.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.learn.dto.ZipCodeResponse;
import com.example.learn.entities.CsvJob;
import com.example.learn.entities.Users;
import com.example.learn.repositories.CsvJobRepository;
import com.example.learn.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProcessingService {

    private final UserRepository userRepository;
    private final AddressService addressService;
    private final CsvJobRepository csvJobRepository;

    public Users createUser(String[] userData, ZipCodeResponse zipCodeResponse, CsvJob job) {
        Users user = new Users();
        user.setFirstName(userData[0]);
        user.setLastName(userData[1]);
        user.setPhoneNumber1(userData[3]);
        user.setPhoneNumber2(userData[4]);
        user.setEmail(userData[5]);
        user.setWebsiteUrl(userData[6]);
        user.setJob(job);
        user.setAddress(addressService.getOrCreateAddress(userData[2], zipCodeResponse));
        return userRepository.save(user);
    }

}