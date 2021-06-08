package com.gm.demo.services;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface CustomerStreams {
    String OUTPUT = "customer-data-out";
    String INPUT = "customer-data-in";

    @Output(OUTPUT)
    MessageChannel outboundCustomerData();

    @Input(INPUT)
    MessageChannel inboundCustomerData();
}
