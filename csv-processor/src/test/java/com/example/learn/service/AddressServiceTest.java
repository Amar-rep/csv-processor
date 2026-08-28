package com.example.learn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.learn.dto.ZippopotamusResponse;
import com.example.learn.entities.Address;
import com.example.learn.repositories.AddressRepository;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    @Test
    void getOrCreatedAddress_addressExist_Test() {
        String zipcode = "12344";
        Address ad1 = new Address();
        ZippopotamusResponse response = new ZippopotamusResponse();
        when(addressRepository.findByZipCode(zipcode)).thenReturn(Optional.of(ad1));

        Address address = addressService.getOrCreateAddressFromZip(zipcode, response);

        assertEquals(address, ad1);
        verify(addressRepository).findByZipCode(zipcode);

    }

    @Test
    void getOrCreatedAddrses_addressNotExist_test() {
        String zipcode = "1234";
        ZippopotamusResponse.Place place = new ZippopotamusResponse.Place();

        place.setPlaceName("palce name");
        place.setState("cal");

        ZippopotamusResponse response = new ZippopotamusResponse();

        response.setCountryAbbreviation("US");
        response.setPlaces(List.of(place));
        Address ad1 = new Address();

        when(addressRepository.findByZipCode(zipcode)).thenReturn(Optional.empty(), Optional.of(ad1));

        Address result = addressService.getOrCreateAddressFromZip(zipcode, response);
        assertEquals(ad1, result);

    }

}
