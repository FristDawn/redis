package com.lucifer.redis.distributedLock.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Lucifer
 * @create 2018-09-08 15:35
 * @desc: redis分布式重入锁锁
 **/
@Component
public class RedisReentrantLock {

    @Autowired
    private RedisTemplate redisTemplate;

    private final ConcurrentMap<Thread, LockData> threadData = new ConcurrentHashMap<>();

    //重试时间
    private static final int retryAwait=300;
    //默认锁过期时间
    private static final int lockTimeout=5000;
    //轮询获取锁过期时间
    private static final int awaitTimeout = 20000;
    //更新锁有效时间的间隔
    private static final int guardTime = 2000;

    private static final String LOCK_ ="LOCK_";
    //当前锁的唯一标识
    private String lockId;

    //加锁时使用lua脚本
    private final String luaScriptOn = ""
                                    + "\nlocal r = tonumber(redis.call('SETNX', KEYS[1],ARGV[1]));"
                                    + "\nredis.call('PEXPIRE',KEYS[1],ARGV[2]);"
                                    + "\nreturn r";

    //解锁时使用的lua脚本
    private final String luaScriptOff = ""
                                    +"\nlocal v = redis.call('GET', KEYS[1]);"
                                    +"\nlocal r= 0;"
                                    +"\nif v == ARGV[1] then"
                                    +"\nr =redis.call('DEL',KEYS[1]);"
                                    +"\nend"
                                    +"\nreturn r";
    //申请延迟锁有效时间的lua脚本
    private final String getLuaScriptDelay=""+
                                            "\nif redis.call('get', KEYS[1]) == ARGV[1] " +
                                            "\nthen return redis.call('expire',KEYS[1],ARGV[2]) " +
                                            "\nelse return 0 end";

    /**
     * 记录当前线程重入次数与是否获取锁
     */
    private static class LockData {
        //当前线程
        final Thread owningThread;
       //获取锁对应的值
        final String lockVal;
        //重入次数
        final AtomicInteger lockCount = new AtomicInteger(1);

        private LockData(Thread owningThread, String lockVal) {
            this.owningThread = owningThread;
            this.lockVal = lockVal;
        }
    }

    /**
     * 加锁
     * @param lockKey 加锁唯一标识入参
     * @param timeout 分布式锁有效时间，出入零或负数按默认值
     * @param unit 时间单位
     * @return 是否加锁成功
     * @throws InterruptedException
     */
    public boolean tryLock (String lockKey, long timeout, TimeUnit unit) throws  InterruptedException{

        if (StringUtils.isEmpty(lockKey)){
            return false;
        }
        //获取当前线程
        Thread thread = Thread.currentThread();
        //判断当前线程是否获取锁，以及重入计数
        LockData lockData = threadData.get(thread);
        if (!Objects.equals(null,lockData)){
            lockData.lockCount.incrementAndGet();
            return true;
        }
         //获取锁开始时间
        final long startTime = System.currentTimeMillis();
        //加锁有效时间
        final long millisToLock = (Objects.equals(null, unit)||timeout <lockTimeout ) ? lockTimeout : unit.toMillis(timeout);
        //redis分布式锁的value值，解锁时用于校验是否为当前用户的锁
        String lockValue = lockKey+ UUID.randomUUID();
        //轮询去获取锁，未获取锁则阻塞一段时间继续去获取，直到获取锁超时返回失败
        for (;;){
            //判断是否存在锁，存在则返回false，不存则新增key值并返回true
           if(createLock(lockKey,lockValue,millisToLock,startTime) == 1){
               LockData data = new LockData(thread,lockValue);
               threadData.put(thread,data);
               this.lockId = lockKey;
               return true;
           }
           if (System.currentTimeMillis() - startTime - retryAwait>awaitTimeout){
               return false;
           }
           //让线程阻塞一断时间
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(retryAwait));
        }
    }

    /**
     * 创建加锁命令
     * @param lockKey 加锁唯一标识入参
     * @param lockValue 锁对应的值
     * @param millisToLock 锁的过期时间
     * @return 是否加锁成功 1 为成功
     */
    public Integer createLock(String lockKey ,String lockValue, Long millisToLock ,Long startTime){

        //执行解锁的lua脚本，根据返回值判断是否成功
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScriptOn, Long.class);
        ArrayList<String> keys = new ArrayList<>(1);
        keys.add(LOCK_+lockKey);
        String values [] = new String[2];
        values[0] = lockValue;
        values[1] = String.valueOf(millisToLock);
        Object execute = redisTemplate.execute(redisScript, keys, values);
        if (Objects.equals(null,execute)){
            return 0;
        }
        //创建守护进程
        Thread guard = new Thread(() -> {

            System.out.println("进入守护线程");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millisToLock - 1000));
            for (;;){
                //阻塞一断时间
                DefaultRedisScript<Long> redisScriptDelay = new DefaultRedisScript<>(getLuaScriptDelay, Long.class);
                ArrayList<String> dKeys = new ArrayList<>(1);
                dKeys.add(LOCK_ + lockKey);
                String dValues[] = new String[2];
                dValues[0] = lockValue;
                dValues[1] = String.valueOf(millisToLock);
                Object res = redisTemplate.execute(redisScriptDelay, keys, values);
                System.out.println("执行守护线程lua脚本");
                if (Long.parseLong(String.valueOf(res)) == 0) {
                    System.out.println("守护线程结束");
                    return;
                }
                if(System.currentTimeMillis() - startTime >= awaitTimeout){
                    System.out.println("持有锁超时,结束守护线程");
                    return;
                }
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(guardTime));

            }

        });
        guard.setDaemon(true);
        guard.start();
        return Integer.parseInt(String.valueOf(execute));


    }

    /**
     * 解锁，使用lua命令，保证解锁过程的原子性，防止解到别人的锁
     */
    public void unLock(){
        //获取当前线程，获取其持有锁的信息
        Thread thread = Thread.currentThread();
        LockData lockData = threadData.get(thread);
        try {
            if (Objects.equals(null,lockData)){
                throw  new IllegalMonitorStateException("当前用户未持有此锁:"+lockId);
            }
            //减少重入次数
            int i = lockData.lockCount.decrementAndGet();
            if (i > 0){
                return;
            }
            if (i < 0){
                throw  new IllegalMonitorStateException("当前用户重入此锁有误:"+lockId);
            }
            //执行解锁的lua脚本，根据返回值判断是否成功
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScriptOff, Long.class);
            ArrayList<String> keys = new ArrayList<>(1);
            keys.add(LOCK_+this.lockId);
            String values [] = new String[1];
            values[0] = lockData.lockVal;
            Object result = this.redisTemplate.execute(redisScript,keys, values);
            int res = Integer.parseInt(String.valueOf(result));
            if (Objects.equals(null,result)|| res == 0){
               throw new IllegalMonitorStateException("当前用户删除此锁有误:"+thread.getName());

            }


        } finally {
            threadData.remove(thread);
        }

    }


}
