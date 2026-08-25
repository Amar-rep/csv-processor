package com.example.learn.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.learn.entities.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

        Optional<Address> findByZipCode(String zipcode);

        @Modifying
        @Query(value = """
                                INSERT INTO address (zip_code,country,city,state)
                                VALUES (:zipcode,:country,:city,:state)
                                ON CONFLICT (zip_code) DO NOTHING
                        """, nativeQuery = true)
        void upsertAddress(@Param("zipcode") String zipcode,
                        @Param("country") String country,
                        @Param("city") String city,
                        @Param("state") String state);
}
