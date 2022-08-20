package com.atguigu.gulimall.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-coupon")
@RequestMapping("coupon/coupon")
public interface CouponsFeignClient {
    @RequestMapping("/member/coupons")
    public R memberCoupons();
}
