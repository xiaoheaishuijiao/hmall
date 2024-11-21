package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartCleanListener {
    private final ICartService cartService;

    @RabbitListener(bindings =@QueueBinding(
            value = @Queue(name = "cart.clear.queue",durable = "true"),
            exchange = @Exchange(name = "trade.topic",type = ExchangeTypes.TOPIC),
            key = {"order.create"}
    ))
    public void deleteCartItemByIds(Collection<Long> itemIds, Message message){
        Long userId = message.getMessageProperties().getHeader("user-info");
        log.debug("messageProperties:"+message.getMessageProperties());
        UserContext.setUser(userId);
        cartService.removeByItemIds(itemIds);
        UserContext.removeUser();
    }
}
