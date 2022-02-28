# distributed-lock
分布式锁的四种实现方式
## 1、基于Mysql实现

### 1.1、简单锁

根据主键约束，插入数据库数据，插入失败，那么获取锁失败，释放锁，删除数据库数据。

缺点：不可以重入，即获取不到锁报错

**获取锁**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Boolean tryLock(String key, long timeout, TimeUnit unit) {
    //尝试获取锁
    int res;
    long begin = System.currentTimeMillis();
    try {
        res = methodLockMapper.insert(getEntity(key));
        long end = System.currentTimeMillis();
        res = (end - begin) <=  unit.toMillis(timeout) && res > 0 ? 1 : -1;
    }catch (Exception e){
        e.printStackTrace();
        log.error("获取锁失败");
        res = -1;
    }
    return res > 0;
}
```

**释放锁**

```java
public void releaseLock(String key) {
    try {
        LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
        methodLock.eq(MethodLock::getMethodName,key);
        int res = methodLockMapper.delete(methodLock);
        log.info("解锁成功，res = {}" , res);
    }catch (Exception e){
        e.printStackTrace();
        log.error("获取锁失败");
    }
}
```

### 1.2、乐观锁

需要2次操作: select版本号一次；update一次。

缺点：无法感知ABA操作，所以使用version字段来判断

**获取锁**

```java

    @Override
    public boolean optimisticLock(String methodName, long timeout, TimeUnit unit) throws Exception {
        LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
        methodLock.eq(MethodLock::getMethodName,methodName);
        methodLock.eq(MethodLock::getState,false);
      //  isContains(methodName);
        // 前提：当前数据必须存在且数据的状态state必须为false
        for(;;){
            MethodLock one = methodLockMapper.selectOne(methodLock);
            if(one != null){
                LambdaQueryWrapper<MethodLock> update = new LambdaQueryWrapper<>();
                update.eq(MethodLock::getMethodName,methodName);
                update.eq(MethodLock::getVersion,one.getVersion());
                update.eq(MethodLock::getState,false);
                one.setState(true);
                one.setVersion(one.getVersion() + 1);
                int res;
                try {
                    res = methodLockMapper.update(one, update);
                }catch (Exception e){
                    //更新失败，继续尝试获取锁
                    e.printStackTrace();
                    log.error("获取锁失败,休息20毫秒继续获取");
                    continue;
                }
                //更新成功获取锁
               if( res > 0){
                   log.info("获取锁成功");
                   return true;
               }
            }
        }
    }
```

**释放锁**

```java
public void releaseOptimisticLock(String methodName) {
    LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
    methodLock.eq(MethodLock::getMethodName,methodName);
    MethodLock one = methodLockMapper.selectOne(methodLock);
    if(one != null){
        if(one.getState()){
            one.setState(false);
            one.setVersion(one.getVersion() - 1);
            methodLock.ge(MethodLock::getVersion,0);
            methodLockMapper.updateById(one);
        }
    }
}
```



### 1.3、悲观锁

核心：使用mysql for update 语句实现，和数据事务共同实现

```java
	// 1.获取锁，不释放事务
    public boolean pessimisticLock(String methodName, long timeout, TimeUnit unit) {
        String sql = "SELECT id from method_lock where method_name = ? for UPDATE";
        //isContains(methodName);
        transaction.set(dataSourceTransactionManager.getTransaction(transactionDefinition));
        long waitTime = 500;
        long futureTime = System.currentTimeMillis() + timeout;
        long reTime = timeout;
        try {
            for (;;){
                try {
                    MethodLock methodLock = methodLockMapper.selectByMethodName(methodName);
                    return methodLock != null;
                }catch (Exception e){
                    e.printStackTrace();
                }
                reTime = futureTime - System.currentTimeMillis();
                if (reTime <= 0)
                    break;
                if (reTime < waitTime) {
                    waitTime = reTime;
                }
            }
            return false;
        }catch (Exception e){
            dataSourceTransactionManager.rollback(transaction.get());
            return false;
        }
    }
```

**释放锁**

```java
public void releasePessimisticLock(String methodName) {
    // 2.释放锁，释放事务
    dataSourceTransactionManager.commit(transaction.get());
}
```

## 2、基于redis实现

### 2.1、简单锁

思想：setValue成功获取锁，（否则循环等待尝试set），释放锁，直接删除当前key，

1.获取锁

```java
public Boolean tryLock(String key, long timeout, TimeUnit unit) {
    Boolean isLocked;
    log.info("尝试获取 redis lock 锁");
    long futureTime = timeout + System.currentTimeMillis();
    String uuid = null;
    for (; ; ) {
        if (threadLocal.get() == null) {
            uuid = UUID.randomUUID().toString();
            threadLocal.set(uuid);
        }
        assert uuid != null;
        isLocked = stringRedisTemplate.opsForValue().setIfAbsent(key, uuid, timeout, unit);
        if (isLocked != null && isLocked) {
            log.info("获取锁成功 = " + uuid);
            return true;
        }
        long now = System.currentTimeMillis();
        if(futureTime - now < 0){
            break;
        }
    }
    throw  new RuntimeException("获取锁超时...");
}
```

2.释放锁

```java
@Override
public void releaseLock(String key) {
    // 判断当前线程所对应的uuid是否与Redis对应的uuid相同，再执行删除锁操作
    log.info("释放当前锁 = " + threadLocal.get());
    if (threadLocal.get().equals(stringRedisTemplate.opsForValue().get(key))) {
        stringRedisTemplate.delete(key);
    }
}
```

3.可重入获取锁

```java
public boolean tryReLock(String key, long timeout, TimeUnit unit) {
    long futureTime = timeout + System.currentTimeMillis();
    String uuid = threadLocal.get();
    if (uuid == null) {
        uuid = UUID.randomUUID().toString();
        threadLocal.set(uuid);
    }
    for (; ; ) {
        log.info("uuid = " + uuid);
        Long res = stringRedisTemplate.opsForHash().increment(key, uuid, 1L);
        if (res > 0) {
            log.info("获取锁成功 = " + uuid);
            return true;
        }
        long now = System.currentTimeMillis();
        if(futureTime - now < 0){
            break;
        }
    }
    throw  new RuntimeException("获取锁失败，超时...");
}
```

4、可重入锁释放

```java
public void releaseReLock(String key) {
    String uuid = threadLocal.get();
    log.info("释放当前锁 = " + uuid);
    long res  = Long.parseLong(String.valueOf(stringRedisTemplate.opsForHash().get(key,uuid )));
    System.out.println("res = " + res);
    if(res > 1){
        stringRedisTemplate.opsForHash().increment(key, threadLocal.get(),-1L);
    }else {
        stringRedisTemplate.opsForHash().delete(key,uuid);
    }
}
```

5、简单锁续约

```java
@Override
@Async
public void renewLock(String key, String value, int time) {
    String v = redisTemplate.opsForValue().get(key);
    if (value.equals(v)){
        int sleepTime = time / 3;
        try {
            Thread.sleep(sleepTime * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        redisTemplate.expire(key,time,TimeUnit.SECONDS);
        renewLock(key,value,time);
    }
}
```

### 2.2、redisson锁

```java
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
```

### 2.3、redissonRed锁（红锁）

```java
public static RLock create (String url, String key){
    Config config = new Config();
    config.useSingleServer().setAddress(url).setPassword("******");
    RedissonClient redissonClient = Redisson.create(config);
    return redissonClient.getLock(key);
}
RedissonRedLock redissonRedLock = new RedissonRedLock(
        create("redis://127.0.0.1:16379","lock_key1"),
        create("redis://127.0.0.1:16380","lock_key2"),
        create("redis://127.0.0.1:16381","lock_key3"));
RedisRedLock redLock = new RedisRedLock(redissonRedLock);
ExecutorService executorService = Executors.newCachedThreadPool();
@SneakyThrows
@Test
public void redisLockTest(){
    int[] count = {0};
    int size = 100;
    CountDownLatch latch = new CountDownLatch(size);
    for (int i = 0; i < size; i++) {
        executorService.submit(() -> {
            try {
                redLock.lock();
                    count[0]++;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                redLock.unlock();
                System.out.println("结果:" + Arrays.toString(count));
            }
        });
    }
    executorService.shutdown();
    executorService.awaitTermination(1,TimeUnit.HOURS);
    System.out.println(Arrays.toString(count));
}
```

## 3、基于zk实现

支持可重入得排他锁

```java
/**
 * 支持可重入的排它锁
 */
@Slf4j
public class ZkDistributedLock extends AbstractLock {

    /**
     * 1.Connect to zk
     */
    private CuratorFramework client;

    private InterProcessLock lock;

    public ZkDistributedLock(String addr, String lockPath) {
        //1. connect
        client = CuratorFrameworkFactory.newClient(addr, new RetryNTimes(5, 5000));
        client.start();
        if (client.getState() == CuratorFrameworkState.STARTED) {
            log.info("zk client start successfully!");
            log.info("zkAddress:{},lockPath:{}", addr, lockPath);
        } else {
            throw new RuntimeException("客户端启动失败。。。");
        }
        this.lock = new InterProcessMutex(client, lockPath);
    }

    @Override
    public void lock() {
        try {
            lock.acquire();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上锁失败...");
        }
    }

    @Override
    public boolean tryLock() {
        boolean res = false;
        try {
            res =  this.lock.acquire(0,TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取锁失败...");
        }
        return res;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        boolean res = false;
        try {
            res =  this.lock.acquire(time,unit);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取锁失败...");
        }
        return res;
    }

    @Override
    public void unlock() {
        try {
            this.lock.release();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("释放锁失败...");
        }
    }
```

## 4、基于etcd实现

```java
/**
 * etcd节点锁数据
 */
@Data
public class LockData {
    private String lockKey;
    private boolean lockSuccess;
    private long leaseId;
    private Thread owning;
    private String lockPath;
    private ScheduledExecutorService executorService;
    final AtomicInteger lockCount = new AtomicInteger(1);

    public LockData() {
    }
    public LockData(Thread owningThread, String lockPath){
        this.owning = owningThread;
        this.lockPath = lockPath;
    }
}
```

```java
import com.google.common.collect.Maps;
import com.only.base.AbstractLock;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lock.LockResponse;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
@Slf4j
public class EtcdDistributedLock extends AbstractLock {
    private Lock lock;
    private final Lease lease;
    private String lockKey;
    private String lockPath;
    private AtomicInteger lockCount;
    /** 租约有效期,防止客户端崩溃，可在租约到期后自动释放锁；另一方面，正常执行过程中，会自动进行续租,单位 ns */
    private final Long leaseTTL;
    /** 续约锁租期的定时任务，初次启动延迟，单位默认为 s,默认为1s，可根据业务定制设置*/
    private Long initDelay = 1L;
    /** 定时任务线程池类 */
    ScheduledExecutorService service = null;
    /** 保存线程与锁对象的映射，锁对象包含重入次数，重入次数的最大限制为Int的最大值 */
    private final ConcurrentMap<Thread, LockData> threadData = Maps.newConcurrentMap();

    public EtcdDistributedLock(Client client, String lockPath, Long leaseTTL, TimeUnit unit) {
        this.lease = client.getLeaseClient();
        this.lockPath = lockPath;
        this.leaseTTL = unit.toNanos(leaseTTL);
        this.service = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void lock() {
        Thread thread = Thread.currentThread();
        LockData lockData = threadData.get(thread);
        if(lockData != null && lockData.isLockSuccess()){ // 得重入了
            int res = lockData.getLockCount().incrementAndGet();
            System.out.println("重入的次数 count = " + res);
            return;
        }
        // 第一次获取锁
        long leaseId = 0;
        try {
            leaseId = lease.grant(TimeUnit.NANOSECONDS.toNanos(leaseTTL)).get().getID();
            // 续租心跳周期
            long period = leaseTTL - leaseTTL / 5;
            // 启动定时任务续约
            service.scheduleWithFixedDelay(new KeepAlive(lease,leaseId),initDelay,period,TimeUnit.NANOSECONDS);
            LockResponse lockResponse = lock.lock(ByteSequence.from(lockKey.getBytes()), leaseId).get();
            if(lockResponse != null){
                lockPath = lockResponse.getKey().toString(StandardCharsets.UTF_8);
                log.info("获取锁成功,锁路径:{},线程:{}",lockPath,thread.getName());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // 获取锁成功，锁对象设置
        LockData newLockData = new LockData(thread, lockKey);
        newLockData.setLeaseId(leaseId);
        newLockData.setExecutorService(service);
        threadData.put(thread, newLockData);
        newLockData.setLockSuccess(true);
    }

    @Override
    public void unlock() {
        Thread thread = Thread.currentThread();
        LockData lockData = threadData.get(thread);
        if (lockData == null){
            throw new IllegalMonitorStateException("You do not own the lock: " + lockKey);
        }
        // 减去次数
        int newLockCount = lockData.lockCount.decrementAndGet();
        if ( newLockCount > 0 ) {
            return;
        }
        if ( newLockCount < 0 ) {
            throw new IllegalMonitorStateException("Lock count has gone negative for lock: " + lockKey);
        }
        try {
            // 释放锁
            if(lockPath != null){
                lock.unlock(ByteSequence.from(lockPath.getBytes())).get();
            }
            // 关闭定时任务
            lockData.getExecutorService().shutdown();
            // 删除租约
            if (lockData.getLeaseId() != 0L) {
                lease.revoke(lockData.getLeaseId());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("解锁失败",e);
        }finally {
            // 移除当前线程资源
            threadData.remove(thread);
        }
    }

    private static class KeepAlive implements Runnable{
        private final Lease lease;
        private final long leaseId;

        public KeepAlive(Lease lease, long leaseId) {
            this.lease = lease;
            this.leaseId = leaseId;
        }

        @Override
        public void run() {
            // 对当前ID进行一次续约
            lease.keepAliveOnce(leaseId);
        }
    }
```