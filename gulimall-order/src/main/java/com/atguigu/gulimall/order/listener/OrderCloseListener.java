package com.atguigu.gulimall.order.listener;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @Description: 定时关闭订单
 **/

@RabbitListener(queues = "order.release.order.queue")
@Service
public class OrderCloseListener {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void listener(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期的订单信息，准备关闭订单" + orderEntity.getOrderSn());
        try {
            orderService.closeOrder(orderEntity);
            // 每个channel发送的消息都是有一个唯一标记的，就是deliveryTag，从1开始递增
            // 消息的标识，false只确认当前一个消息收到，true确认所有consumer获得的消息（成功消费，消息从队列中删除 ）
            // channel 的 basicAck 方法的第一个参数就是当前消费者的标签，
            // 第二个参数表示是否启用 RabbitMQ 的消费者自动 ACK ，如果要想手动对消费端进行 ACK ，那么这个属性一定不要开启，即将该属性的值设置为 false ，关闭消费端的 autoAck 才行。
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            //告诉服务器不应该由我处理，或者拒绝处理，扔掉
            //接收端在发送reject命令的时候可以选择是否要重新放回queue中。如果没有其他接收者监控这个queue的话，要注意一直无限循环发送的危险。
            //BasicReject方法第一个参数是消息的DeliveryTag，对于每个Channel来说，每个消息都会有一个DeliveryTag，一般用接收消息的顺序来表示：1,2,3,4 等等。第二个参数是是否放回queue中，requeue。
            //BasicReject一次只能拒绝接收一个消息，而BasicNack方法可以支持一次0个或多个消息的拒收，并且也可以设置是否requeue。
            //channel.BasicNack(3, true, false);
            //在第一个参数DeliveryTag中如果输入3，则消息DeliveryTag小于等于3的，这个Channel的，都会被拒收
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}
