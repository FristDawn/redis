package com.lucifer.redis.distributedLock;

/**
 * @author Lucifer
 * @create 2018-09-08 13:53
 * @desc: 回调函数
 **/
public interface Callback {

    public Object onGetLock() throws InterruptedException;

    public Object onTimeout() throws InterruptedException;
}
