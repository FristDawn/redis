package com.lucifer.redis.DTO;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Lucifer
 * @create 2018-09-04 9:56
 * @desc:
 **/
@Data
@Accessors(chain = true)
public class User {

    private String name;

    private String age;

    private String amount;
}
