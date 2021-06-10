package com.gm.demo.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.JsonLoader;
import com.gm.demo.models.CustomerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayloadService {
    private static ObjectNode template;
    private static ObjectNode bodyJsonTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CustomerStreams customerStreams;

    @PostConstruct
    public void init() throws IOException {
        URL resource = getClass().getClassLoader().getResource("template.json");
        template = (ObjectNode) JsonLoader.fromURL(resource);

        String body = template.path("command").path("body").toString();
        bodyJsonTemplate = (ObjectNode) objectMapper.readTree(objectMapper.readValue(body, JsonNode.class).asText());
    }

    public boolean process(String fileUrl, CustomerData customer) {
        try {
            return customerStreams.outboundCustomerData()
                    .send(MessageBuilder
                            .withPayload(this.addCustomerData(customer))
                            .setHeaderIfAbsent("fileUrl", fileUrl)
                            .setHeaderIfAbsent("uuid", customer.getUuid())
                            .setHeaderIfAbsent("VIN", customer.getVin())
                            .setHeaderIfAbsent("customerID", customer.getCustomerID())
                            .build());
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    private JsonNode addCustomerData(CustomerData data) throws JsonProcessingException {
        ObjectNode payload = template.deepCopy();
        ObjectNode bodyJson = bodyJsonTemplate.deepCopy();

        bodyJson.put("customerID", data.getCustomerID());
        bodyJson.put("uuid", data.getUuid());

        ObjectNode vinNode = objectMapper.createObjectNode();
        vinNode.put("key", "VIN");
        vinNode.put("value", data.getVin());

        ArrayNode contentParamsEncoded = (ArrayNode) bodyJson.path("ingestionCommandEventInfo").path("contentParamsEncoded");
        contentParamsEncoded.add(vinNode);

        ((ObjectNode) payload.path("command")).replace("body", new TextNode(objectMapper.writeValueAsString(objectMapper.valueToTree(bodyJson))));
        return payload;
    }

}
