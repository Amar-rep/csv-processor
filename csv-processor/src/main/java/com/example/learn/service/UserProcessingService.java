package com.example.learn.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.learn.dto.UserResultResponse;
import com.example.learn.dto.ZipCodeResponse;
import com.example.learn.dto.ZippopotamusResponse;
import com.example.learn.entities.CsvJob;
import com.example.learn.entities.Users;
import com.example.learn.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProcessingService {

    private final UserRepository userRepository;
    private final AddressService addressService;

    private final CsvJobService csvJobService;

    // for local zipDb server testing only
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

    // external api
    public Users createUserFromZippopotamus(
            String[] userData,
            ZippopotamusResponse response,
            CsvJob job) {

        Users user = new Users();
        user.setFirstName(userData[0]);
        user.setLastName(userData[1]);
        user.setPhoneNumber1(userData[3]);
        user.setPhoneNumber2(userData[4]);
        user.setEmail(userData[5]);
        user.setWebsiteUrl(userData[6]);
        user.setJob(job);
        user.setAddress(addressService.getOrCreateAddressFromZip(userData[2], response));

        return userRepository.save(user);
    }

    public List<UserResultResponse> findUsersByJobId(UUID jobId) {
        csvJobService.findByJobId(jobId);
        return userRepository.findByJobId(jobId).stream().map(UserResultResponse::from).toList();
    }

}
