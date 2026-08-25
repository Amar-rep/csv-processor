package com.example.learn.entities;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="users")
@Getter
@Setter
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private CsvJob job;

    private String firstName;
    private String lastName;
    @Column(name = "phone_number_1")
    private String phoneNumber1;
    @Column(name="phone_number_2")
    private String phoneNumber2;
    private String email;
    private String websiteUrl;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;
}
