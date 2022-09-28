package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public String app_id = "2021000121663845";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public String merchant_private_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCNJani6k/BWkRtSt8CTlsMmvKtkGp4boY7hP8xeqh2SPQmjo1GWhr+A2bVc+X6IojmpeQhBxIkH7c7oiiMhv3LDL8KJ05J86QPuI4fMWz3TSBWcr8dLrFDwkfZQnvQBp2thbsZve2unTRljAQ/L0UeVm2UqqeDmPDixGOUm/5tS1BgdzuGlEFjlkhX9qTNiyo2u+LNHeuDSIw+0RjKqKmMHhs5cJRbR9v9qZxA/40qNoBoCNQLIYdSy465eoacHOtmcVIBSnHzgfeeRaZ+5Xt3u4Vu+RsfT3qlPtDWZM5tLXqX1aNim58c6l0oXghILklvct5P0QsDYNNkBWSRjtelAgMBAAECggEACv2Gx+t0wFp1/4iX6scNIeMiyE75a103yISnlkO1wFgX4rSw3WyAs4yyQiafHqX3wPXj2IqDaBqsGtGalK6iLmBUs8axuuJkwPCe6GDh2jPAM9sFdJFBLkR/cbRSjbUmK+/AzNVUH4eQH+QSJJVarW0PZUcQehe7kfy2wEiRPBNFv5rt00ETuaofsIjPdoQ/r6i3arcc8wzF40wERuVBNJ8FxmwM7uoXnxzZHArK9eTy8MO3sC86y+vseWrdgEv7MQh961B1RZ3K0uy42mUqUteQaT7dqpVRU/j/FtybBTK4ekPlzukdqix6NlT+x+PqfT+PDon/2UF5frMW+s8QIQKBgQDxezbJhrlJCO0X4REnAOR/zGjvFHe2n82Gv3POIgGXoCp68iVt6xYeFLGeIYcdl133BE58Tx+C8VAs0r6LOJlLDsBzo5O3rJaQR/QS0ZQkI2mLEsxLL3c4TtV+si9jDR9ZtzN/P8UBo2nRF+ZxtPS0O0cK9ZYyPTYvXaDlWJnNCQKBgQCVoiUiEyykcD12w9Ousgbs1ypQNO93c5KnLkzO5vulUTn9al5lcwCaFLC+t8fzIEUa/MDKnupW5a9Vy0ayVowHooFQJ1B1cdvhTmpFHDwvWKPjJHnsNVOF2ODdXFxBGDrK/jZ64jpEahgH4o3oGfzP4/PTC+rC8FcmpmUDxIS4vQKBgDwjJDzB2WjP2UPGICBnf4A4JrXLkhif6W4yRaPDIkRhddLSdPHPxt1M6ALqdJyw8oyuvOEGT2GruPzAqwU+3iYsEU97IDFwwLpDwQvit5LcYs9oqgnbU0RplY+MznJLdVBBaIi7F9PVn2ecHR6aIwqzbHjVOkdeqRzYo1YaX4sJAoGAMd1nIoPhTKta0ElSMdWCGUta/n+ljdkl9jCnyAuRsPXwrVLVQlmgybg7blMwfwUyAlxYoy1ySqMgWyby2FufLqwpC90OIxVegSbwozdVlzGi6/8r4lujCefMh/hPhiHc9ejhnvHlGQeCGp6VS9PdPCprBtnh0jTl1it9XlQPJsECgYBFYAHKKLOwGrkyRL2DVtx8lCEIfTzd1At/T7SP7hIPuCRFsD48YZrXbG7ySwRs8qpMnW4DHAKuzWrSSuTo00Cvua7Lzqp8L5lvgWARrpXwpHQkYQIMQorUadWEY1yIqnYXMgskia3UYMebfU2sRMrSiP6jRV+De9sMEqR5rJvscA==";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhwNZYbLTkPUWo7/FTL99bq/4K8sRfuiuOUtSWJ2Y/JqCeoihHwQa3eics5yUDAlYpJ2knYv3HvUZank0kByuwYE3Kv51wXIv2/C8FWDRBC4ojXydUj+AQoZD93Ad2Xbem1hdzFuFEXnZmh7I1j4bWv0a8z+ZRf+yT71x4gvHWsnaKLdMNZ0Jxvc3CtrIgfXpJfRsgp2B8wusJSn+vg5EtkmVWecCS4FKWAPLNnsWTbsf/dcLwE5WJmoAUwdPgvCCJ7uPrUlInTdKf6OxpSbtTDdthVpe/5NhkNqF/yH3rFuY/iyJyc3SO59EkOL1XqlbbPyqfJybCd5dhMq3xsug4wIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    public String notify_url = "https://a926-240e-3a1-a36-f00-70d3-f53e-52ce-f19d.ap.ngrok.io/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    public String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private String sign_type = "RSA2";

    // 字符编码格式
    private String charset = "utf-8";

    //订单超时时间
    private String timeout = "1m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    public String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"timeout_express\":\"" + timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应：" + result);

        return result;
    }
}

