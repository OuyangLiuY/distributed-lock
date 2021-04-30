package com.only.etcd.lock;

import lombok.Data;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class LockData {
    private String lockKey;
    private boolean lockSuccess;
    private long leaseId;
    private Thread owning;
    private String lockPath;
    private ScheduledExecutorService executorService;
    final AtomicInteger lockCount = new AtomicInteger(1);

    public LockData() {
    }
    public LockData(Thread owningThread, String lockPath){
        this.owning = owningThread;
        this.lockPath = lockPath;
    }
}
