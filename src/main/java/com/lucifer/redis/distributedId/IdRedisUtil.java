package com.lucifer.redis.distributedId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Lucifer
 * @create 2018-09-3 11:58
 * @desc:  redis方案，生成分布式下唯一的ID
 **/
@Component
public class IdRedisUtil {

    @Autowired
    private RedisTemplate redisTemplate ;

    private static IdRedisUtil instance;
    private static final Logger logger = LoggerFactory.getLogger(IdRedisUtil.class);
    private static final String key = "order_id_";

    public static IdRedisUtil getInstance(){
        if (instance == null){
            instance = new IdRedisUtil();
        }
        return instance;
    }
    public String getIdPre(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String dayCode = String.format("%1$03d",day);
        String hourCode = String.format("%1$02d",hour);
        return (year-2000)+dayCode+hourCode;

    }

    public String getId(String preFix){


        String orderId = null;
        try {

            if(redisTemplate.opsForValue().get(key+preFix) == null ){
                redisTemplate.opsForValue().set(key+preFix,"0",1,TimeUnit.DAYS);
            }

            Long incr = redisTemplate.opsForValue().increment(key + preFix, 1l);

            orderId = preFix+String.format("%1$05d",incr);

            return orderId;
        } catch (Exception e) {
            logger.error("生成id异常："+e.getMessage());
        }
        return orderId;
    }

    public String id(){
        return getId(getIdPre(new Date()));
    }
}
