package com.only.redis.lock;

import lombok.SneakyThrows;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedLockTest {

    public static RLock create (String url, String key){
        Config config = new Config();
        config.useSingleServer().setAddress(url).setPassword("******");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient.getLock(key);
    }
    RedissonRedLock redissonRedLock = new RedissonRedLock(
            create("redis://127.0.0.1:16379","lock_key1"),
            create("redis://127.0.0.1:16380","lock_key2"),
            create("redis://127.0.0.1:16381","lock_key3"));
    RedisRedLock redLock = new RedisRedLock(redissonRedLock);
    ExecutorService executorService = Executors.newCachedThreadPool();
    @SneakyThrows
    @Test
    public void redisLockTest(){
        int[] count = {0};
        int size = 100;
        CountDownLatch latch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            executorService.submit(() -> {
                try {
                    redLock.lock();
                        count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redLock.unlock();
                    System.out.println("结果:" + Arrays.toString(count));
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1,TimeUnit.HOURS);
        System.out.println(Arrays.toString(count));
    }
}
