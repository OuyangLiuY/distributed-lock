package com.only.test;

import com.only.dao.mapper.MethodLockMapper;
import com.only.lock.MySqlDistributedLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = com.only.MySqlLockApplication.class)
public class MySqlDistributedLockTest {
    @Autowired
    MySqlDistributedLock mySqlDistributedLock;
    @Resource
    MethodLockMapper methodLockMapper;

    @Test
    public void testLock() {
        System.out.println(mySqlDistributedLock.simpleLock("testOneLock", 100L, TimeUnit.SECONDS));
    }

    @Test
    public void mysqlLockTest() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        for (int i = 0; i < 50; i++) {
            executorService.submit(() -> {
                try {
                    mySqlDistributedLock.simpleLock("mysqlLockTest", 100L, TimeUnit.SECONDS);
                    //Thread.sleep(finalI);
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mySqlDistributedLock.releaseLock("mysqlLockTest");
                }
            });

        }
        System.out.println(Arrays.toString(count));
    }


    @Test
    public void pessimisticLockTest() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        for (int i = 0; i < 20; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    mySqlDistributedLock.pessimisticLock("mysqlLockTest",100L,TimeUnit.SECONDS);
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                     mySqlDistributedLock.releaseLock("mysqlLockTest");
                }
            });
        }
        System.out.println(Arrays.toString(count));
    }
}
