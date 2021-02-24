package com.only.controller;

import com.only.lock.MySqlDistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MySqlLockController {

    @Autowired
    MySqlDistributedLock mySqlDistributedLock;

    @GetMapping(value = "mysql/lock/get")
    public Object getMySqlLock(){
        return mySqlDistributedLock.getLockData();
    }
}
