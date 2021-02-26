package com.only.redis.controller;

import com.only.base.DistributedLock;
import com.only.redis.service.RedisDistributeLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class RedisLockController {

    @Autowired
    RedisDistributeLock redisDistributeLock;


    @GetMapping(value = "redis/lock/test")
    public Object getMySqlLock(){
        //redisLockTest();
        redisReLockTest();
        return true;
    }

    public void redisLockTest(){
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        int size = 1000;
        CountDownLatch latch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            executorService.submit(() -> {
                try {
                    Boolean lock = redisDistributeLock.tryLock("redisLockTest", 3000L, TimeUnit.MILLISECONDS);
                    if(lock){
                        count[0]++;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisDistributeLock.releaseLock("redisLockTest");
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

    public void redisReLockTest(){
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        int size = 100;
        CountDownLatch latch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            executorService.submit(() -> {
                try {
                    boolean lock = redisDistributeLock.tryReLock("redisLockTest", 3000L, TimeUnit.MILLISECONDS);
                    if(lock){
                        try{
                            boolean reLock = redisDistributeLock.tryReLock("redisLockTest", 3000L, TimeUnit.MILLISECONDS);
                            if(reLock){
                                count[0]++;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            redisDistributeLock.releaseReLock("redisLockTest");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisDistributeLock.releaseReLock("redisLockTest");
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
