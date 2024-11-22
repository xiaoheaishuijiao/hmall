package com.hmall.item.rabbitMQ;

import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RabbitMQTest {
    @Autowired
    private IItemService itemService;

}
