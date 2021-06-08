package com.gm.demo.models;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class FileProcessStatus {

    private String fileUrl;

    private AtomicInteger total = new AtomicInteger(0);;
    private AtomicInteger failed = new AtomicInteger(0);
    private AtomicInteger success = new AtomicInteger(0);

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private boolean readingComplete = false;

    public FileProcessStatus(String fileUrl) {
        this.fileUrl = fileUrl;
    }


    public Integer getFailed() {
        return failed.get();
    }

    public Integer increaseFailed() {
        return this.failed.incrementAndGet();
    }

    public Integer getSuccess() {
        return success.get();
    }

    public Integer increaseSuccess() {
        return this.success.incrementAndGet();
    }

    public Integer getTotal() {
        return total.get();
    }

    public Integer increaseTotal() {
        return this.total.incrementAndGet();
    }

    public boolean isComplete() {
        return readingComplete && (total.get() == (failed.get() + success.get()));
    }
}
