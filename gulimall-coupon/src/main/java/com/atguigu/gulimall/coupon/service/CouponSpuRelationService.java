package com.atguigu.gulimall.coupon.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.coupon.entity.CouponSpuRelationEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 优惠券与产品关联
 *
 * @author zhaoxiang
 * @email 1084691005@qq.com
 * @date 2022-08-20 15:26:29
 */
public interface CouponSpuRelationService extends IService<CouponSpuRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

