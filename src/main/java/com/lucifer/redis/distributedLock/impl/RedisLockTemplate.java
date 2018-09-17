package com.lucifer.redis.distributedLock.impl;

import com.lucifer.redis.distributedLock.Callback;
import com.lucifer.redis.distributedLock.DistributeLockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author Lucifer
 * @create 2018-09-08 15:33
 * @desc: 分布式锁模板
 **/
@Component
@Slf4j
public class RedisLockTemplate implements DistributeLockTemplate {


    @Autowired
    private RedisReentrantLock redisReentrantLock;
    @Override
    public Object execute(String lockKey, int timeout, Callback callback) {

        boolean getLock = false;
        try {
            if (redisReentrantLock.tryLock(lockKey,timeout, TimeUnit.MILLISECONDS)){
                getLock = true;
                log.info("当前用户获取锁 "+Thread.currentThread().getName());
                return callback.onGetLock();
            }else {
                log.info("当前用户获取锁超时 "+Thread.currentThread().getName());
                return callback.onTimeout();
            }
        } catch (InterruptedException ie) {
            log.error(ie.getMessage(),ie);
            Thread.currentThread().interrupt();
        } catch (Exception e){
            log.error(e.getMessage(),e);
        }finally {
            if (getLock){
                redisReentrantLock.unLock();
                log.info("当前用户已解锁 "+Thread.currentThread().getName());
            }
        }
        return null;
    }
}
