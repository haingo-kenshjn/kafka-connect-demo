package com.gm.demo.services;

import com.gm.demo.models.CustomerData;
import com.gm.demo.repository.CustomerDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerDataRepository customerDataRepository;

    public List<CustomerData> save(List<CustomerData> customerData) {
        customerDataRepository.saveAll(customerData);
        return customerData;
    }

    public List<CustomerData> getAllTutorials() {
        return customerDataRepository.findAll();
    }
}
