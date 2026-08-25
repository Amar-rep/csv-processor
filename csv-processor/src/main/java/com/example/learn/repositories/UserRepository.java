package com.example.learn.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.learn.entities.Users;


public interface UserRepository  extends JpaRepository<Users,Long>{
     List<Users> findByJobId(UUID jobId);
}
