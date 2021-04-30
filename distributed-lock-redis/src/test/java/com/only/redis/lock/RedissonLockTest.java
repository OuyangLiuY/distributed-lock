package com.only.redis.lock;

import lombok.SneakyThrows;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedissonLockTest {

    public RedissonClient getRedissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://118.89.165.94:16379").setPassword("Survey@2019");//(93*3n4f5V5G
        return Redisson.create(config);
    }
    RedisLock redisLock = new RedisLock(getRedissonClient(),"redisson-lock-1");
    ExecutorService executorService = Executors.newCachedThreadPool();
    @SneakyThrows
    @Test
    public void redisLockTest(){
        int[] count = {0};
        int size = 100;
        CountDownLatch latch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            //  RedisLock redisLock = new RedisLock(getRedissonClient(),"redisson-lock-1");
            executorService.submit(() -> {
                try {
                    redisLock.lock();
                    count[0]++;
                  /*  boolean res = redisLock.tryLock(1000L, TimeUnit.MILLISECONDS);
                    if(res){
                        count[0]++;
                    }else {
                        System.out.println("获取锁失败： cur = " + count[0]);
                    }*/
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisLock.unlock();
                    System.out.println("结果:" + Arrays.toString(count));
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1,TimeUnit.HOURS);
        System.out.println(Arrays.toString(count));
    }
}
