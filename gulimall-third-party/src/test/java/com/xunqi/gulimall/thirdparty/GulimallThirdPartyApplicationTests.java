package com.xunqi.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.aliyuncs.http.HttpResponse;
import com.xunqi.gulimall.thirdparty.component.SmsComponent;
import com.xunqi.gulimall.thirdparty.utils.HttpUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTests {

   /* @Resource
    private SmsComponent smsComponent;

    @Test
    public void sendSmsCode() {
        smsComponent.sendCode("15580220501", "134531");
    }

    @Test
    public void testUpload() throws FileNotFoundException {
        // Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = "oss-cn-beijing.aliyuncs.com";
        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
        String accessKeyId = "LTAI4G66cCNM2t7LKE79RaY3";
        String accessKeySecret = "wd0KVDLCO1vVXq4q9aIPTXY7AP7rdW";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传文件流。
        InputStream inputStream = new FileInputStream("C:\\Users\\Jerry\\Desktop\\1.png");

        ossClient.putObject("gulimall-clouds", "1.png", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        System.out.println("上传成功...");
    }
    }*/
    @Autowired
    OSSClient ossClient;

    @Test
    public void testUpload() throws FileNotFoundException {

        InputStream inputStream = new FileInputStream("/Users/zhaoxiang/Desktop/1121656927941_.pic.jpg");
        ossClient.putObject("gulimall-zhaoxiang", "heiheihei.jpg", inputStream);

        ossClient.shutdown();
        System.out.println("上传完成");
    }

    @Test
    public void sendMsg(){
        String host = "https://dfsns.market.alicloudapi.com";
        String path = "/data/send_sms";
        String method = "POST";
        String appcode = "61187ae12418490595ceb4a0ba212f31";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:19990227");
        bodys.put("phone_number", "19851787162");
        bodys.put("template_id", "TPL_0000");


        try {
            HttpResponse response = (HttpResponse) HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Autowired
    SmsComponent smsComponent;

    @Test
    public void testMsg2(){
        smsComponent.sendCode("19851787162","code:9421");
    }

}

