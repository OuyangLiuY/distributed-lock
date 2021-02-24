package com.only.test;

import com.only.lock.MySqlDistributedLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = com.only.test.MySqlLockApplication.class)
public class MySqlDistributedLockTest {
    @Autowired
    MySqlDistributedLock mySqlDistributedLock;

    @Test
    public void testLock(){

        System.out.println(mySqlDistributedLock.getLock("testLock",100L, TimeUnit.SECONDS));
    }

    @Test
    public void mysqlLockTest(){
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] count = {0};
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executorService.submit(()->{
                try {
                    //mySqlDistributedLock.getLock("mysqlLockTest",100L,TimeUnit.SECONDS);
                    //Thread.sleep(finalI);
                    count[0] ++ ;
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                   // mySqlDistributedLock.releaseLock("mysqlLockTest");
                    System.out.println("dd");
                }
            });

        }
        System.out.println(Arrays.toString(count));
    }
}
