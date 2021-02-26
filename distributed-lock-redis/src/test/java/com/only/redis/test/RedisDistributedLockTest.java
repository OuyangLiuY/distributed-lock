package com.only.redis.test;

import com.only.base.DistributedLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedisDistributedLockTest {

    @Autowired
    DistributedLock distributedLock;

    @Test
    public void redisLockTest(){
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        int size = 100;
        CountDownLatch latch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            executorService.submit(() -> {
                try {
                    Boolean lock = distributedLock.tryLock("redisLockTest", 1000L, TimeUnit.MILLISECONDS);
                    if(lock){
                        count[0]++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    distributedLock.releaseLock("redisLockTest");
                    System.out.println("结果:" + Arrays.toString(count));
                }
            });
            //latch.countDown();
        }
        /*try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        System.out.println(Arrays.toString(count));
    }
}
