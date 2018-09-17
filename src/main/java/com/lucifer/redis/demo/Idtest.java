package com.lucifer.redis.demo;

import com.lucifer.redis.RedisApplication;
import com.lucifer.redis.distributedId.IdRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author xingxin
 * @create 2018-09-03 17:27
 * @desc:
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RedisApplication.class)
@Slf4j
public class Idtest {

    @Test
    public void test(){

        for(int i=0 ; i<= 100 ;i++){
            String id = IdRedisUtil.getInstance().id();

            System.out.println(id);
        }

        try {
            Thread.currentThread().sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(int i=0 ; i<= 100 ;i++){
            String id = IdRedisUtil.getInstance().id();
            System.out.println(id);
        }

    }
}
