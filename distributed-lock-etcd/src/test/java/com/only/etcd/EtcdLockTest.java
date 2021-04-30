package com.only.etcd;

import com.only.etcd.lock.EtcdDistributedLock;
import io.etcd.jetcd.Client;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EtcdLockTest {
    private Client client;
    private String key = "/etcd/lock";
    private static final String server = "http://xxxx:xxxx";
    private final ExecutorService executorService = Executors.newFixedThreadPool(10000);

    @Before
    public void before() throws Exception {
        initEtcdClient();
    }

    private void initEtcdClient(){
        client = Client.builder().endpoints(server).build();
    }

    @Test
    public void testEtcdDistributeLock() throws InterruptedException {
        int[] count = {0};
        for (int i = 0; i < 1000; i++) {
            executorService.submit(() -> {
                final EtcdDistributedLock lock = new EtcdDistributedLock(client, key,20L, TimeUnit.SECONDS);
                try {
                    lock.lock();
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lock.unlock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.err.println("执行结果: " + count[0]);
    }

}
