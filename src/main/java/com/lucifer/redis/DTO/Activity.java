package com.lucifer.redis.DTO;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author Lucifer
 * @create 2018-09-03 13:53
 * @desc: 活动
 **/
@Data
@Accessors(chain = true)
public class Activity implements Serializable {

    private String activityCode;

    private String activityName;

    private String type;

}
