package com.atguigu.gulimall.ware.vo;

import lombok.Data;


@Data
public class LockStockResultVo {

    //锁的商品id
    private Long skuId;
    //锁的数量
    private Integer num;
    //是否锁定成功
    private Boolean locked;

}
