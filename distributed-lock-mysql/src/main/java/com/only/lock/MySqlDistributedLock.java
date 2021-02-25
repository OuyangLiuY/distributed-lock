package com.only.lock;

import com.only.base.DistributedLock;
import com.only.dao.entity.MethodLock;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface MySqlDistributedLock extends DistributedLock {

    /**
     * 获取锁，最简单的实现方式
     * 缺点：不可以重入，即获取不到锁报错
     * @param methodName
     * @param timeout
     * @param unit
     * @return
     */
    boolean simpleLock(String methodName, long timeout, TimeUnit unit);

    /**
     * 乐观锁方式获取锁
     * 缺点：无法感知ABA操作，所以使用version字段来判断
     * ①这种操作方式，使原本一次的update操作，必须变为2次操作: select版本号一次；update一次。增加了数据库操作的次数。
     * ②如果业务场景中的一次业务流程中，多个资源都需要用保证数据一致性，那么如果全部使用基于数据库资源表的乐观锁，就要让每个资源都有一张资源表，
     * 这个在实际使用场景中肯定是无法满足的。而且这些都基于数据库操作，在高并发的要求下，对数据库连接的开销一定是无法忍受的。
     * ③乐观锁机制往往基于系统中的数据存储逻辑，因此可能会造成脏数据被更新到数据库中。在系统设计阶段，我们应该充分考虑到这些情况出现的可能性，
     * 并进行相应调整，如将乐观锁策略在数据库存储过程中实现，对外只开放基于此存储过程的数据更新途径，而不是将数据库表直接对外公开。
     * @param methodName
     * @param timeout
     * @param unit
     * @return
     */
    boolean optimisticLock(String methodName,long timeout, TimeUnit unit) throws Exception;

    void releaseOptimisticLock(String methodName);

    /**
     * 悲观锁方式获取锁
     * @param methodName
     * @param timeout
     * @param unit
     * @return
     */
    boolean pessimisticLock(String methodName,long timeout, TimeUnit unit);

    void releasePessimisticLock(String methodName);


    List<MethodLock> getLockData();
}
