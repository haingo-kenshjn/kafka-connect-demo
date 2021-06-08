package com.gm.demo.services;

import com.gm.demo.enums.RecordStatus;
import com.gm.demo.models.CustomerData;
import com.gm.demo.models.FileProcessStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
public class CSVService {
    public static String TYPE = "text/csv";
    static String[] HEADERs = {"VIN", "customerID"};
    static String[] OUTPUT_HEADERs = {"uuid", "VIN", "customerID", "success", "failed"};
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.from(ZoneOffset.UTC));

    Map<String, CSVPrinter> cachedWriter = new HashMap<>();

    public static boolean hasCSVFormat(MultipartFile file) {

        if (!TYPE.equals(file.getContentType())) {
            return false;
        }

        return true;
    }

    public List<CustomerData> csvToCustomerData(InputStream is, FileProcessStatus fileProcessStatus,
                                                Function<CustomerData, Void> processCustomerData) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            List<CustomerData> customerData = new ArrayList<>();

            for (CSVRecord csvRecord : csvParser) {
                CustomerData customer = CustomerData.builder()
                        .vin(csvRecord.get("VIN"))
                        .customerID(csvRecord.get("customerID"))
                        .build();
                customerData.add(customer);
                // log.debug("read csv uuid {}", customer.getUuid());
                processCustomerData.apply(customer);
                fileProcessStatus.increaseTotal();
            }

            log.info("File {} processing, reading csv file completed.Total count: {}, Start Time: {}",
                    fileProcessStatus.getFileUrl(), fileProcessStatus.getTotal(), fileProcessStatus.getStartTime());
            fileProcessStatus.setReadingComplete(true);
            return customerData;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

    public FileProcessStatus createWriter(FileProcessStatus fileProcessStatus) {
        try {
            String fileName = fileProcessStatus.getFileUrl();
            String url = buildFileName(fileName);

            BufferedWriter writer = Files.newBufferedWriter(
                    Paths.get("./result/" + url),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                    .withHeader(OUTPUT_HEADERs));
            csvPrinter.flush();

            fileProcessStatus.setFileUrl(url);
            cachedWriter.put(fileProcessStatus.getFileUrl(), csvPrinter);
            return fileProcessStatus;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public boolean writeCustomerUploadResult(FileProcessStatus fileProcessStatus, CustomerData data, RecordStatus recordStatus) {
        CSVPrinter printer = cachedWriter.get(fileProcessStatus.getFileUrl());

        List<String> values = new ArrayList<>();
        values.add(data.getUuid().toString());
        values.add(data.getVin());
        values.add(data.getCustomerID());

        if (recordStatus == RecordStatus.SUCCESS) {
            values.add("1");
            values.add("0");
        } else if (recordStatus == RecordStatus.FAILED) {
            values.add("0");
            values.add("1");
        }

        try {
            // synchronized write to prevent wrong write order data on same file
            synchronized (printer) {
                printer.printRecord(values);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }

        return true;
    }

    public void tryCloseCSVPrinter(FileProcessStatus fileProcessStatus) {
        try {
            cachedWriter.get(fileProcessStatus.getFileUrl()).close();
            cachedWriter.remove(fileProcessStatus);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }


    @NotNull
    private String buildFileName(String fileName) {
        return fileName.substring(0, fileName.length() - 4)
                + "_" + formatter.format(Instant.now())
                + "_" + UUID.randomUUID().hashCode()
                + ".csv";
    }

}
