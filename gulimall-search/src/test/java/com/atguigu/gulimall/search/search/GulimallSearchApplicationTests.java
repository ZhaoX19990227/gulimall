package com.atguigu.gulimall.search.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {
    @Autowired
    private RestHighLevelClient client;

    @Test
    void contextLoads() throws IOException {
        IndexRequest indexRequest = new IndexRequest("zhaoxiang");
        User user = new User("zhaoxiang",12);
        indexRequest.source(JSON.toJSONString(user), XContentType.JSON);
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(JSON.toJSONString(index));
    }

    @Data
    @AllArgsConstructor
    static class User{
        private String name;
        private Integer age;
    }
}
