package com.atguigu.gulimall.product;

import com.aliyun.oss.OSSClient;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;
    @Autowired
    OSSClient ossClient;

    @Autowired
    CategoryService categoryService;

    @Test
    public void testFindPath(){
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
}
