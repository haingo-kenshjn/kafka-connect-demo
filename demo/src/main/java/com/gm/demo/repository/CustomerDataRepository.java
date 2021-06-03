package com.gm.demo.repository;

import com.gm.demo.models.CustomerData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerDataRepository extends JpaRepository<CustomerData, UUID> {
}
