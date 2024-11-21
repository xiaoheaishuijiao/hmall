package com.hmall.trade.listener;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDelayMessageListener {

    private final IOrderService orderService;

    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_ORDER_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME,type = ExchangeTypes.DIRECT,delayed = "true"),
            key = {MQConstants.DELAY_ORDER_KEY}
    ))
    public void listenOrderDelayMessage(Long orderId) {
        // 1.查询订单当前状态是否为已支付？
        Order order = orderService.getById(orderId);
        // 1.1已支付，直接返回
        if (ObjectUtil.isNull(order)||order.getStatus()!=1) {
            return;
        }
        // 1.2未支付，查询支付流水
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        //2.查询出的订单流水是已支付
        //修改订单状态
        if (payOrderDTO!=null&&payOrderDTO.getStatus()==3){
            orderService.markOrderPaySuccess(orderId);
        }
        //3.查询出是未支付
        else {
            log.info("超时未支付，为你取消订单");
            orderService.cancelOrder(orderId);
        }
    }
}
