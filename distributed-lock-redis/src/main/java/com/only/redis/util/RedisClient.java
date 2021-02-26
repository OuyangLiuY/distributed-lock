package com.only.redis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisClient {

    @Autowired
    private RedisTemplate redisTemplate;

    //redis生成自增主键  add by zhuangyang  20200526
    public String getInceId() {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy");
        Date date=new Date();
        String formatDate=sdf.format(date);
        String key="serialUUIDKey";
        Long incr = getIncr(key, getCurrent2TodayEndMillisTime());
        if(incr==0) {
            incr = getIncr(key, getCurrent2TodayEndMillisTime());//从000001开始
        }
        DecimalFormat df=new DecimalFormat("000000");//序列号
        return formatDate+df.format(incr);
    }
    public Long getIncr(String key, long liveTime) {
        //log.info("自增id的过期时间：{}",liveTime);
        RedisAtomicLong entityIdCounter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        Long increment = entityIdCounter.getAndIncrement();
		/*if ((null == increment || increment.longValue() == 0) && liveTime > 0) {
			//初始设置过期时间
			//entityIdCounter.expire(365, TimeUnit.DAYS);//单位毫秒
		}*/
        return increment;
    }
    //现在到今天结束的毫秒数
    public Long getCurrent2TodayEndMillisTime() {
        Calendar todayEnd = Calendar.getInstance();
        //Calendar.HOUR 12小时制
        //HOUR_OF_DAY 24小时制
        //todayEnd.set(Calendar.YEAR,1);
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        //return todayEnd.getTimeInMillis()-new Date().getTime();
        return todayEnd.getTimeInMillis()-System.currentTimeMillis();
    }


    /**
     * 存放string类型
     *
     * @param key
     *            key
     * @param data
     *            数据
     * @param timeout
     *            超时间
     */
    public void setString(String key, String data, Long timeout) {
        try {

            redisTemplate.opsForValue().set(key, data);
            if (timeout != null) {
                redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
            }

        } catch (Exception e) {

        }

    }

    /**
     * 存放string类型
     *
     * @param key
     *            key
     * @param data
     *            数据
     */
    public void setString(String key, String data) {
        setString(key, data, null);
    }

    /**
     * 根据key查询string类型
     *
     * @param key
     * @return
     */
    public String getString(String key) {
        String value = (String) redisTemplate.opsForValue().get(key);
        return value;
    }

    /**
     * 根据对应的key删除key
     *
     * @param key
     */
    public Boolean delKey(String key) {
        return redisTemplate.delete(key);

    }

    /**
     * 指定缓存过期时间
     *
     * @param key  键
     * @param time 时间（秒）
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("指定缓存过期时间时异常,指定key：[{}];过期时间：[{}]", key, time);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     *
     * @param key key不能为null
     * @return 时间（秒），返回0表示永久有效
     */
    public long getExpire(String key) {
        Assert.notNull(key, "键key不能为null");
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键 key
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("判断key是否存在时异常，key: [{}]", key);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除某个缓存
     *
     * @param key 可以一个或者多个
     */
    public void delete(String... key) {
        if (key != null && key.length != 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /**
     * 字符串键获取
     *
     * @param key 键key
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置字符串键
     *
     * @param key   键
     * @param value 值
     * @return 是否设置成功
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("放入字符串键时出错。键：[{}],");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置有过期时间的字符串键
     *
     * @param key   键
     * @param value 值
     * @param time  有效时长
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
                return true;
            } else {
                return set(key, value);
            }
        } catch (Exception e) {
            log.error("设置带有过期时间的字符串键值对时出错。键：[{}], 值：[{}], 过期时间：[{}]", key, value, time);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key 键
     * @param num 递增因子
     */
    public long increment(String key, long num) {
        if (num < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, num);
    }

    /**
     * 递减
     *
     * @param key 键
     * @param num 递减因子
     */
    public long decrement(String key, long num) {
        if (num < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -num);
    }

    /**
     * @param key  键，不能为null
     * @param hkey 值中的key，不能为null
     */
    public Object hget(String key, String hkey) {
        Assert.notNull(key, "hash键不能为null");
        Assert.notNull(hkey, "hash值的key不能为null");
        return redisTemplate.opsForHash().get(key, hkey);
    }

    /**
     * 获取key中所有的键值对
     *
     * @param key 键
     * @return map
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 为key设置多个键值对
     *
     * @param key 键
     * @param map 键值对
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("为key：[{}]设置多个键值对时出错。", key);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 为key设置多个有过期时间的键值对
     *
     * @param key  键
     * @param map  键值对
     * @param time 有效时间（秒）
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("为key：[{}]设置多个带有过期时间的键值对时出错。", key);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中加入一个键值对，不存在则创建
     *
     * @param key    键
     * @param hkey   hhasKey
     * @param hvalue hashValue
     */
    public boolean hset(String key, String hkey, Object hvalue) {
        try {
            redisTemplate.opsForHash().put(key, hkey, hvalue);
            return true;
        } catch (Exception e) {
            log.error("为key：[{}]添加数据时出错。hhasKey：[{}]，hashValue：[{}]", key, hkey, hvalue);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中加入一个有过期时间的键值对，不存在则创建
     *
     * @param key    键
     * @param hkey   hhasKey
     * @param hvalue hashValue
     * @param time   有效时长，单位秒
     */
    public boolean hset(String key, String hkey, Object hvalue, long time) {
        try {
            redisTemplate.opsForHash().put(key, hkey, hvalue);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("为key：[{}]添加数据时出错。hhasKey：[{}]，hashValue：[{}]，有效时长：[{}]", key, hkey, hvalue, time);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键，不能为null
     * @param hkey hashKey，不能为null
     */
    public void hdelete(String key, String hkey) {
        Assert.notNull(key, "hash键不能为null");
        Assert.notNull(hkey, "hash值的key不能为null");
        redisTemplate.opsForHash().delete(key, hkey);
    }

    /**
     * 判断key中是否有该hashKey
     *
     * @param key  键，不能为null
     * @param hkey hashKey，不能为null
     */
    public boolean hhasKey(String key, String hkey) {
        Assert.notNull(key, "hash键不能为null");
        Assert.notNull(hkey, "hash值的key不能为null");
        return redisTemplate.opsForHash().hasKey(key, hkey);
    }

    /**
     * 递增
     *
     * @param key  键
     * @param hkey hashKey
     * @param l    递增因子,不能为负
     */
    public long hincrement(String key, String hkey, long l) {
        if (l < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForHash().increment(key, hkey, l);
    }

    public long hdecrement(String key, String hkey, long l) {
        if (l < 0) {
            throw new RuntimeException("递减因子不能小于0");
        }
        return redisTemplate.opsForHash().increment(key, hkey, -l);
    }

    /**
     * 递增
     *
     * @param key  键
     * @param hkey hashKey
     * @param d    递增因子，不能为负
     */
    public double hincrement(String key, String hkey, double d) {
        if (d < 0.0) {
            throw new RuntimeException("递增因子必须大于0.0");
        }
        return redisTemplate.opsForHash().increment(key, hkey, d);
    }

    /**
     * 递减
     *
     * @param key  键
     * @param hkey hashKey
     * @param d    递减因子，不能为负
     */
    private double hdecrement(String key, String hkey, double d) {
        if (d < 0.0) {
            throw new RuntimeException("递减因子必须大于0.0");
        }
        return redisTemplate.opsForHash().increment(key, hkey, -d);
    }

    /**
     * 根据key获取Set集合值
     *
     * @param key 键
     */
    public Set<Object> sget(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("获取 set 对象时出错，key：[{}]", key);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     */
    public boolean shasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("判断是否含有Set键的值时出错，键：[{}]", key);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 放入多个set值
     *
     * @param key    键
     * @param values 值，多个或一个
     * @return 返回成功个数，0说明一个都没添加成功
     */
    public long sset(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("添加多个set数据时出错，键：[{}]", key);
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * @param key    键
     * @param time   时间（秒）
     * @param values 一个或多个值
     */
    public long ssetAndExpired(String key, long time, Object... values) {
        try {
            long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            log.error("添加多个带有过期时间的set数据时出错，键：[{}]", key);
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     */
    public long sgetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值，一个或者多个
     * @return 成功移除的值的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取list缓存的内容
     *
     * @param key        键
     * @param startIndex 起始索引
     * @param endIndex   结束索引
     */
    public List<Object> lget(String key, long startIndex, long endIndex) {
        try {
            return redisTemplate.opsForList().range(key, startIndex, endIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     */
    public long lgetSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取list中指定索引出的值
     *
     * @param key   键
     * @param index 大于0时，顺序查找；小于0时，倒序查找
     */
    public Object lgetByIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置list键值对
     *
     * @param key   键
     * @param value 值
     */
    public boolean lset(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 存储带有过期时间的list键值对
     *
     * @param key   键
     * @param value 值
     * @param time  有效时间
     * @return
     */
    public boolean lset(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 存储多个值的list键值对
     *
     * @param key    键
     * @param values 值，集合
     */
    public boolean lset(String key, List<Object> values) {
        try {
            redisTemplate.opsForList().leftPushAll(key, values);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 存储带有过期时间的多个值的list键值对
     *
     * @param key    键
     * @param values 值，集合
     */
    public boolean lset(String key, List<Object> values, long time) {
        try {
            redisTemplate.opsForList().leftPushAll(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 修改某个index下的值
     *
     * @param key   键
     * @param index 索引
     * @param value 修改后的值
     */
    public boolean lupdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除指定个数的list键值对的值
     *
     * @param key   键
     * @param count 移除个数
     * @param value 值
     * @return 返回成功移除的个数
     */
    public long lremove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取所有key
     */
    public Set<String> getAllKey(){

        Set<String> keys = null;
        try {
            keys = redisTemplate.keys("*");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keys;
    }

    /**
     * 开启Redis 事务
     *
     */
    public void begin() {
        // 开启Redis 事务权限
        redisTemplate.setEnableTransactionSupport(true);
        // 开启事务
        redisTemplate.multi();

    }

    /**
     * 提交事务
     *
     */
    public void exec() {
        // 成功提交事务
        redisTemplate.exec();
    }

    /**
     * 回滚Redis 事务
     */
    public void discard() {
        redisTemplate.discard();
    }

}
