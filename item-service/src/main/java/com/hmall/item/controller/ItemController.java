package com.hmall.item.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.client.SearchClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.constants.MQConstants;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Api(tags = "商品管理相关接口")
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final IItemService itemService;

    private final RabbitTemplate rabbitTemplate;

    @ApiOperation("分页查询商品")
    @GetMapping("/page")
    public PageDTO<ItemDTO> queryItemByPage(PageQuery query,@RequestHeader(value = "truth",required = false) String truth) {
        System.out.println("truth="+truth);
        // 1.分页查询
        Page<Item> result = itemService.page(query.toMpPage("update_time", false));
        // 2.封装并返回
        return PageDTO.of(result, ItemDTO.class);
    }

    @ApiOperation("根据id批量查询商品")
    @GetMapping
    public List<ItemDTO> queryItemByIds(@RequestParam("ids") List<Long> ids) throws IOException {
        // 模拟网络延迟
//        ThreadUtil.sleep(500);
//        return searchClient.searchByIds(ids);
        return null;
    }

    @ApiOperation("根据id查询商品")
    @GetMapping("{id}")
    public ItemDTO queryItemById(@PathVariable("id") Long id) throws IOException {
//        return BeanUtils.copyBean(searchClient.searchById(id), ItemDTO.class);
        return null;
    }

    @ApiOperation("新增商品")
    @PostMapping
    @Transactional
    public void saveItem(@RequestBody ItemDTO item) {
        //1.新增
        Long id = itemService.saveAndGetId(BeanUtils.copyBean(item, Item.class));
        //2.更新ES,MQ实现
        if (id!=null){
            rabbitTemplate.convertAndSend(MQConstants.ITEM_EXCHANGE_NAME,MQConstants.ITEM_UPDATE_KEY,id,
                    message -> {
                        message.getMessageProperties().setDelay(1000);
                        return message;
                    }
            );
        }
    }

    @ApiOperation("更新商品状态")
    @PutMapping("/status/{id}/{status}")
    public void updateItemStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status){
        //1.更新商品状态
        Item item = new Item();
        item.setId(id);
        item.setStatus(status);
        boolean isSuccess = itemService.updateById(item);
        //2.通过MQ通知ES实现更新
        if (isSuccess){
            rabbitTemplate.convertAndSend(MQConstants.ITEM_EXCHANGE_NAME,MQConstants.ITEM_STATUS_UPDATE_KEY,item,
                    message -> {
                        message.getMessageProperties().setDelay(1000);
                        return message;
                    }
            );
        }
    }

    @ApiOperation("更新商品")
    @PutMapping
    public void updateItem(@RequestBody ItemDTO item) {
        //1.不允许修改商品状态，所以强制设置为null，更新时，就会忽略该字段
        item.setStatus(null);
        //2.更新
        boolean isSuccess = itemService.updateById(BeanUtils.copyBean(item, Item.class));
        //2.通过MQ通知ES实现更新
        if (isSuccess) {
            rabbitTemplate.convertAndSend(MQConstants.ITEM_EXCHANGE_NAME,MQConstants.ITEM_ALL_UPDATE_KEY,item);
        }
    }

    @ApiOperation("根据id删除商品")
    @DeleteMapping("{id}")
    public void deleteItemById(@PathVariable("id") Long id) {
        //1.删除商品
        boolean isSuccess = itemService.removeById(id);
        //2.通过MQ通知ES实现更新
        if (isSuccess) {
            rabbitTemplate.convertAndSend(MQConstants.ITEM_EXCHANGE_NAME,MQConstants.ITEM_DELETE_KEY,id);
        }
    }

    @ApiOperation("批量扣减库存")
    @PutMapping("/stock/deduct")
    public void deductStock(@RequestBody List<OrderDetailDTO> items){
        itemService.deductStock(items);
    }

    @ApiOperation("订单取消回复库存")
    @PutMapping("/stock/recover")
    public void recoverStock(@RequestBody List<OrderDetailDTO> items){
        itemService.recoverStock(items);
    }
}
