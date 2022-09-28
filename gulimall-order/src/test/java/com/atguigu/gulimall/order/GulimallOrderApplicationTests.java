package com.atguigu.gulimall.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@Slf4j
@RunWith(SpringRunner.class)
public class GulimallOrderApplicationTests {

    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void createExchange() {
        DirectExchange directExchange = new DirectExchange("hello-tatan", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", "hello-tatan");
    }

    @Test
    public void createQueue() {
        Queue queue = new Queue("hello-tatan-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功", "hello-queue");
    }

    @Test
    public void bind() {
        Binding binding =
                new Binding("hello-tatan-queue", Binding.DestinationType.QUEUE, "hello-tatan", "hello.java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]成功", "hello-binding");
    }

    @Test
    public void sendMsg() {
        String msg = "你好啊！";
        rabbitTemplate.convertAndSend("hello-tatan", "hello.java", msg.getClass());
        log.info("消息发送成功{}", msg);
    }
}
