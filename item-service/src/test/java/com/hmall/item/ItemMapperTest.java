package com.hmall.item;

import com.hmall.item.domain.po.Item;
import com.hmall.item.mapper.ItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")  // 指定使用local环境
public class ItemMapperTest {

    @Autowired
    private ItemMapper itemMapper;

    @Test
    public void testMySqlConnect(){
        Item item = itemMapper.selectById(626738);
        System.out.println(item);
    }

}
