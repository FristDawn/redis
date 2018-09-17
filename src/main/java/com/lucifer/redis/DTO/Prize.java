package com.lucifer.redis.DTO;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Lucifer
 * @create 2018-09-03 15:57
 * @desc:
 **/
@Data
@Accessors(chain = true)
public class Prize {

    private String type;

    private String amount;

    private Integer count;
}
