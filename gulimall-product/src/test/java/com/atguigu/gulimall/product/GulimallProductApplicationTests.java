package com.atguigu.gulimall.product;

import com.aliyun.oss.OSSClient;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;
    @Autowired
    OSSClient ossClient;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redissonClient;

    @Autowired
    CategoryService categoryService;
    @Autowired
    AttrGroupService attrGroupService;

    @Test
    public void testFindPath() {
        Long[] path = categoryService.findCatelogPath(225L);
        log.info("完整路径:{}", Arrays.asList(path));
    }


    @Test
    public void testUpload() throws FileNotFoundException {

        InputStream inputStream = new FileInputStream("/Users/zhaoxiang/Desktop/1121656927941_.pic.jpg");
        ossClient.putObject("gulimall-zhaoxiang", "1121656927941_.pic.jpg", inputStream);

        ossClient.shutdown();
        System.out.println("上传完成");
    }

    @Test
    public void redis() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("xiaopang", "nihao!" + UUID.randomUUID().toString());

        String xiaopang = ops.get("xiaopang");
        System.out.println(xiaopang);
    }

    @Test
    public void test() {
        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupService.getAttrGroupWithAttrsBySpuId(13L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);
    }
}
