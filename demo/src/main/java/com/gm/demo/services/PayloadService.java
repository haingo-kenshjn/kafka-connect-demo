package com.gm.demo.services;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

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

    public void process(CustomerData customer) {
        try {
            customerStreams.outboundCustomerData()
                    .send(MessageBuilder
                            .withPayload(this.addCustomerData(customer))
                            .build());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public JsonNode addCustomerData(CustomerData data) throws JsonProcessingException {
        ObjectNode payload = template.deepCopy();
        ObjectNode bodyJson = bodyJsonTemplate.deepCopy();

        bodyJson.put("customerID", data.getCustomerID());
        bodyJson.put("uuid", data.getUuid().toString());

        ObjectNode vinNode = objectMapper.createObjectNode();
        vinNode.put("key", "VIN");
        vinNode.put("value", data.getVin());

        ArrayNode contentParamsEncoded = (ArrayNode) bodyJson.path("ingestionCommandEventInfo").path("contentParamsEncoded");
        contentParamsEncoded.add(vinNode);

        ((ObjectNode) payload.path("command")).replace("body", new TextNode(objectMapper.writeValueAsString(objectMapper.valueToTree(bodyJson))));
        return payload;
    }

}
