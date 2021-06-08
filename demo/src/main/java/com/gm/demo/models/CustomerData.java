package com.gm.demo.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerData {

    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    private String vin;

    private String customerID;
}
