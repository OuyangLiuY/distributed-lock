package com.only.test;

import com.only.lock.MySqlDistributedLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
}
