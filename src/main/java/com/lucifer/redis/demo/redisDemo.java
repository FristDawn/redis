package com.lucifer.redis.demo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lucifer.redis.DTO.Activity;
import com.lucifer.redis.DTO.Prize;
import com.lucifer.redis.DTO.User;
import com.lucifer.redis.RedisApplication;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


/**
 * @author Lucifer
 * @create 2018-09-03 10:29
 * @desc: redis基本数据结构案例
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RedisApplication.class)
@Slf4j
public class redisDemo {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String  ACTIVITY_ = "ACTIVITY_" ;

    private static final String  INSURANCE_ = "INSURANCE_";

    private static final String  USER = "USER";

    private static final String RANK = "RANK";
    @Test
    public void demo1(){

        log.info("String类型数据结构实例一");
        Activity activity = new Activity();
        activity.setActivityCode("ZQJ").setActivityName("中秋节").setType("获客活动");
        redisTemplate.opsForValue().set(ACTIVITY_+activity.getActivityCode(), JSONObject.toJSONString(activity));
        String activityStr  = (String) redisTemplate.opsForValue().get(ACTIVITY_ + activity.getActivityCode());
        Activity back = JSONObject.parseObject(activityStr, Activity.class);
        log.info("当前活动详情为："+back);

    }

    @Test
    public void demo2(){
        log.info("String类型数据结构实例二");
        Activity activity1 = new Activity().setActivityCode("ZQJ").setActivityName("中秋节").setType("获客活动");
        Activity activity2 = new Activity().setActivityCode("GQJ").setActivityName("国庆节").setType("获客活动");
        Activity activity3 = new Activity().setActivityCode("TXB").setActivityName("泰行保").setType("投保活动");
        ArrayList<Activity> activities = new ArrayList<>();
        activities.add(activity1);
        activities.add(activity2);
        activities.add(activity3);
        redisTemplate.opsForValue().set(ACTIVITY_+"TOTAL", JSONArray.toJSONString(activities));
        String activitiesStr = (String) redisTemplate.opsForValue().get(ACTIVITY_ + "TOTAL");
        List<Activity> backs = JSONArray.parseArray(activitiesStr, Activity.class);
        backs.forEach(e -> System.out.println(e));
    }
    @Test
    public void demo3(){
        log.info("List类型数据结构示例一");
        Prize prize1 = new Prize().setType("一等奖").setAmount("1").setCount(10);
        Prize prize2 = new Prize().setType("二等奖").setAmount("2").setCount(20);
        HashMap<String, Integer> map = new HashMap<>();
        map.put(prize1.getAmount(),prize1.getCount());
        map.put(prize2.getAmount(),prize2.getCount());
        List<String> shuffle = shuffle(map);
        Long size = redisTemplate.opsForList().leftPushAll(ACTIVITY_+"TEST",shuffle);
        System.out.println("size = "+size);
    }

    @Test
    public void demo4(){
        log.info("List类型数据结构示例二");
        String amount = (String) redisTemplate.opsForList().rightPop(ACTIVITY_ + "TEST");
        Long size = redisTemplate.opsForList().size(ACTIVITY_ + "TEST");
        System.out.println("amount = "+amount+"&&size = "+size);
    }

    @Test
    public void demo5(){
        log.info("hash类型数据结构示例一");
        redisTemplate.opsForHash().put(INSURANCE_+"TOTAL", UUID.randomUUID().toString(),JSONObject.toJSONString(new User().setName("A").setAge("18").setAmount("2000")));
        redisTemplate.opsForHash().put(INSURANCE_+"TOTAL", UUID.randomUUID().toString(),JSONObject.toJSONString(new User().setName("B").setAge("19").setAmount("3000")));
        redisTemplate.opsForHash().put(INSURANCE_+"TOTAL", "123456789",JSONObject.toJSONString(new User().setName("C").setAge("20").setAmount("4000")));

        String insuranceStr = (String) redisTemplate.opsForHash().get(INSURANCE_ + "TOTAL", "123456789");
        User user = JSONObject.parseObject(insuranceStr, User.class);
        System.out.println(user);

    }
    @Test
    public void demo6(){
        log.info("set类型数据结构示例");
        redisTemplate.opsForSet().add(USER,"1");
        redisTemplate.opsForSet().add(USER,"2");
        redisTemplate.opsForSet().add(USER,"3");
        Long size = redisTemplate.opsForSet().size(USER);
        System.out.println("size = "+size);
    }
    @Test
    public void demo7(){
        log.info("zSet类型数据结构示例");
        redisTemplate.opsForZSet().add(RANK,"1",1.0);
        redisTemplate.opsForZSet().add(RANK,"2",2.0);
        redisTemplate.opsForZSet().add(RANK,"3",3.0);
        Set<String> set = redisTemplate.opsForZSet().reverseRange(RANK, 0, -1);
        set.forEach(e -> System.out.println(e));
    }
    @Test
    public void demo8(){
       log.info("geo类型数据结构示例");
        redisTemplate.opsForGeo().add("cityGeoKey", new Point(116.405285, 39.904989), "北京");
        redisTemplate.opsForGeo().add("cityGeoKey", new Point(121.472644, 31.231706), "上海");
        Distance distance = redisTemplate.opsForGeo().distance("cityGeoKey", "北京", "上海", RedisGeoCommands.DistanceUnit.KILOMETERS);
        System.out.println(distance);

    }

    private List<String> shuffle(HashMap<String, Integer> map) {

        ArrayList<String> li = new ArrayList<>();
        map.forEach((k,v)->{
            for (int i = 0; i < v; i++) {
                li.add(k);
            }
       });

        Random current = ThreadLocalRandom.current();
        for (int i = li.size()-1; i >0 ; i--) {
            int index = current.nextInt(i + 1);
            String a = li.get(index);
            li.set(index,li.get(i));
            li.set(i,a);
        }
        return li;
    }

}
