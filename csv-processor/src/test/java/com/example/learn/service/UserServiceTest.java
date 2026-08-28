package com.example.learn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.learn.dto.UserResultResponse;
import com.example.learn.entities.Address;
import com.example.learn.entities.CsvJob;
import com.example.learn.entities.Users;
import com.example.learn.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressService addressService;
    @Mock
    private CsvJobService csvJobService;
    @InjectMocks
    private UserProcessingService userProcessingService;

    @Test
    void findUserByJobID_test() {
        UUID jobId = UUID.randomUUID();
        CsvJob job = new CsvJob();

        Users user = new Users();
        user.setFirstName("test name");
        Address address = new Address();
        address.setZipCode("1234");
        user.setAddress(address);

        when(csvJobService.findByJobId(jobId)).thenReturn(job);
        when(userRepository.findByJobId(jobId)).thenReturn(List.of(user));

        List<UserResultResponse> result = userProcessingService.findUsersByJobId(jobId);

        assertEquals(address.getZipCode(), result.get(0).address().zipCode());
        assertEquals(user.getFirstName(), result.get(0).firstName());
    }
}
