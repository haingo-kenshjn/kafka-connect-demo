package com.gm.demo.services;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface CustomerStreams {
    String OUTPUT = "customer-data-out";

    @Output(OUTPUT)
    MessageChannel outboundCustomerData();

}
