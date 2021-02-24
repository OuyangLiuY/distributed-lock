package com.only.lock;

import com.only.base.DistributedLock;
import com.only.dao.entity.MethodLock;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface MySqlDistributedLock extends DistributedLock {


    boolean getLock(String methodName,long timeout, TimeUnit unit);

    List<MethodLock> getLockData();
}
