package com.lucifer.redis.demo;

import com.lucifer.redis.RedisApplication;
import com.lucifer.redis.distributedLock.Callback;
import com.lucifer.redis.distributedLock.impl.RedisLockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * @author Lucifer
 * @create 2018-09-10 18:31
 * @desc: 测试redis分布式锁
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RedisApplication.class)
@Slf4j
public class redisLockTest {
    @Autowired
    private RedisLockTemplate redisLockTemplate;

    /**
     *  模拟并发量进行测试
     * @throws InterruptedException
     */
    @Test
    public void test() throws InterruptedException {
        //模拟100并发
        int size=10;
        final CountDownLatch startCountDownLatch = new CountDownLatch(1);
        final CountDownLatch endDownLatch=new CountDownLatch(size);
        //模拟剩余库存
        final  int count [] = {10};

        for (int i = 0; i <size ; i++) {
            new Thread(() -> {
                try {
                    startCountDownLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
             //  final int sleepTime = ThreadLocalRandom.current().nextInt(3) * 500;
                final int sleepTime = 2000;
                redisLockTemplate.execute("lucifer", 3000, new Callback() {
                    //正常获取锁
                    @Override
                    public Object onGetLock() throws InterruptedException {

                        log.info("@@@@@@@@@@@@"+Thread.currentThread().getName() +"获取锁成功@@@@@@@@@@@@");
                        Thread.sleep(sleepTime);
                       log.info("@@@@@@@@@@@@"+Thread.currentThread().getName() +"沉睡 "+sleepTime+"@@@@@@@@@@@@");
                        count[0]--;
                        log.info("@@@@@@@@@@@@"+Thread.currentThread().getName() +"库存剩余量 "+count[0]+"@@@@@@@@@@@@");
                        endDownLatch.countDown();
                        return null;
                    }
                    //获取锁超时
                    @Override
                    public Object onTimeout() throws InterruptedException {
                        log.info("@@@@@@@@@@@@"+Thread.currentThread().getName() +"获取锁超时 @@@@@@@@@@@@");
                        endDownLatch.countDown();
                        return null;
                    }
                });
            }).start();
        }
        startCountDownLatch.countDown();
        endDownLatch.await();

        Thread.sleep(2000);
        System.out.println("最终剩余量"+count[0]);

    }

    /**
     * 多线程进行测试
     * @throws InterruptedException
     */
    @Test
    public void test2() throws InterruptedException {

        //模拟剩余库存
        final int count[] = {10};
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                redisLockTemplate.execute("lucifer", 3000, new Callback() {
                    //正常获取锁
                    @Override
                    public Object onGetLock() throws InterruptedException {

                        System.out.println("@@@@@@@@@@@@" + Thread.currentThread().getName() + "获取锁成功@@@@@@@@@@@@");
                        count[0]--;
                        System.out.println("@@@@@@@@@@@@" + Thread.currentThread().getName() + "库存剩余量 " + count[0] + "@@@@@@@@@@@@");
                        return null;
                    }

                    //获取锁超时
                    @Override
                    public Object onTimeout() throws InterruptedException {
                        System.out.println("@@@@@@@@@@@@" + Thread.currentThread().getName() + "获取锁超时 @@@@@@@@@@@@");
                        return null;
                    }
                });
            }
            ).start();
        }


        Thread.sleep(50000);
        System.out.println(count[0]);
    }

    /**
     * 同步情况下进行测试
     */
    @Test
    public void test3(){

        final int count[] = {10};
        for (int i = 0; i <10 ; i++) {
            redisLockTemplate.execute("lucifer", 30000, new Callback() {
                //正常获取锁
                @Override
                public Object onGetLock() throws InterruptedException {

                    System.out.println("@@@@@@@@@@@@" + Thread.currentThread().getName() + "获取锁成功@@@@@@@@@@@@");
                    count[0]--;
                    System.out.println("@@@@@@@@@@@@" + Thread.currentThread().getName() + "库存剩余量 " + count[0] + "@@@@@@@@@@@@");
                    return null;
                }

                //获取锁超时
                @Override
                public Object onTimeout() throws InterruptedException {
                    System.out.println("@@@@@@@@@@@@" + Thread.currentThread().getName() + "获取锁超时 @@@@@@@@@@@@");
                    return null;
                }
            });
        }




    }
}
