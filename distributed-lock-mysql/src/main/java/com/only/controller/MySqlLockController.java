package com.only.controller;

import com.only.lock.MySqlDistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class MySqlLockController {

    @Autowired
    MySqlDistributedLock mySqlDistributedLock;

    @GetMapping(value = "mysql/lock/test")
    public Object getMySqlLock(){
        //pessimisticLockTest();
        optimisticLockTest();
        return mySqlDistributedLock.getLockData();
    }

    /**
     * 悲观锁获取分布式锁,相比与乐观锁方式数据库压力较小
     */
    public void pessimisticLockTest() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        for (int i = 0; i < 2000; i++) {
            executorService.submit(() -> {
                try {
                    mySqlDistributedLock.pessimisticLock("pessimisticLock",100L, TimeUnit.SECONDS);
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("结果" + Arrays.toString(count));
                    mySqlDistributedLock.releasePessimisticLock("pessimisticLock");
                }
            });
        }
        /*try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        System.out.println("最终结果" + Arrays.toString(count));
    }

    /**
     *  mysql乐观锁获取数据，缺点：特别特别慢，数据库压力特别大
     */
    public void optimisticLockTest() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        for (int i = 0; i < 1000; i++) {
            executorService.submit(() -> {
                try {
                    mySqlDistributedLock.optimisticLock("optimisticLock",100L, TimeUnit.SECONDS);
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("结果" + Arrays.toString(count));
                    mySqlDistributedLock.releaseOptimisticLock("optimisticLock");
                }
            });
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("最终结果" + Arrays.toString(count));
    }
}
