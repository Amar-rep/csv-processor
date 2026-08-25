package com.example.learn.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.learn.entities.Address;


public interface  AddressRepository extends JpaRepository<Address,Long>{

    @Query(value="""
            INSERT INTO address (zipcode) 
            VALUES (:zipcode)
            ON CONFLICT (zipcode) DO NOTHING
    """,
            nativeQuery=true
    )
    Optional<Address> findByZipCode(String zipCode);
}
