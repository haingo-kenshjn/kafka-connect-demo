package com.gm.demo.services;

import com.gm.demo.enums.RecordStatus;
import com.gm.demo.models.CustomerData;
import com.gm.demo.models.FileProcessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.kafka.support.KafkaSendFailureException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessService {
    private final CSVService csvService;
    private final PayloadService payloadService;
    private final ApplicationContext context;

    Map<String, FileProcessStatus> cacheFileUploadStatus = new HashMap<>();

    public FileProcessStatus startProcessFile(String fileName, InputStream is) {
        FileProcessStatus fileProcessStatus = new FileProcessStatus(fileName);
        csvService.createWriter(fileProcessStatus);

        fileProcessStatus.setStartTime(ZonedDateTime.now(ZoneOffset.UTC));
        cacheFileUploadStatus.put(fileProcessStatus.getFileUrl(), fileProcessStatus);
        log.info("File {} processing. Start Time: {}", fileProcessStatus.getFileUrl(), fileProcessStatus.getStartTime());

        csvService.csvToCustomerData(is, fileProcessStatus,
                // context.getBean to go through spring proxy and apply @Async annotation
                (customerData -> context.getBean(FileProcessService.class).process(fileProcessStatus, customerData)));
        return fileProcessStatus;
    }

    @Async(value = "csvFileProcessExecutor")
    public Void process(FileProcessStatus fileProcessStatus, CustomerData customerData) {
        boolean result = payloadService.process(fileProcessStatus.getFileUrl(), customerData);

        if (!result) {
            updateFileProcessStatus(fileProcessStatus, customerData, false);
        }

        return null;
    }

    @ServiceActivator(inputChannel = "errorChannel")
    public void handle(final ErrorMessage em) throws IOException {
        log.error("Error caught" + em.toString());

        if (em.getPayload() instanceof KafkaSendFailureException) {
            KafkaSendFailureException ex = (KafkaSendFailureException) em.getPayload();
            String fileUrl = new String(ex.getRecord().headers().headers("fileUrl").iterator().next().value());
            String uuid = new String(ex.getRecord().headers().headers("uuid").iterator().next().value());
            String vin = new String(ex.getRecord().headers().headers("VIN").iterator().next().value());
            String customerID = new String(ex.getRecord().headers().headers("customerID").iterator().next().value());

            CustomerData failed = CustomerData.builder()
                    .uuid(uuid)
                    .vin(vin)
                    .customerID(customerID)
                    .build();

            FileProcessStatus fileProcessStatus = cacheFileUploadStatus.get(fileUrl);
            updateFileProcessStatus(fileProcessStatus, failed, false);
        }
    }

    @StreamListener(CustomerStreams.INPUT)
    public void handleMessage(Message message) {
        try {
            if (!message.getHeaders().containsKey("fileUrl")) {
                return;
            }

            Map<String, Object> headers = message.getHeaders();
            String fileUrl = (String) headers.get("fileUrl");
            String uuid = (String) headers.get("uuid");
            String vin = (String) headers.get("VIN");
            String customerID = (String) headers.get("customerID");

            CustomerData success = CustomerData.builder()
                    .uuid(uuid)
                    .vin(vin)
                    .customerID(customerID)
                    .build();

            FileProcessStatus fileProcessStatus = cacheFileUploadStatus.get(fileUrl);
            if (fileProcessStatus != null) {
                updateFileProcessStatus(fileProcessStatus, success, true);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Nullable
    private void updateFileProcessStatus(FileProcessStatus fileProcessStatus, CustomerData customerData, boolean result) {
        if (result) {
            csvService.writeCustomerUploadResult(fileProcessStatus, customerData, RecordStatus.SUCCESS);
            fileProcessStatus.increaseSuccess();
        } else {
            csvService.writeCustomerUploadResult(fileProcessStatus, customerData, RecordStatus.FAILED);
            fileProcessStatus.increaseFailed();
        }

        if (fileProcessStatus.isComplete()) {
            csvService.tryCloseCSVPrinter(fileProcessStatus);
            cacheFileUploadStatus.remove(fileProcessStatus.getFileUrl());
            fileProcessStatus.setEndTime(ZonedDateTime.now(ZoneOffset.UTC));

            log.info("File {} completed. Total {}, Success {}, Failed {}, Time: {} ms, Start : {}, End: {}, ",
                    fileProcessStatus.getFileUrl(),
                    fileProcessStatus.getTotal(),
                    fileProcessStatus.getSuccess(),
                    fileProcessStatus.getFailed(),
                    ChronoUnit.MILLIS.between(fileProcessStatus.getStartTime(), fileProcessStatus.getEndTime()),
                    fileProcessStatus.getStartTime(), fileProcessStatus.getEndTime());
        } else {
            log.info("File {} processing. Total {}, Success {}, Failed {}",
                    fileProcessStatus.getFileUrl(),
                    fileProcessStatus.getTotal(),
                    fileProcessStatus.getSuccess(),
                    fileProcessStatus.getFailed());
        }
    }
}
