package com.hmall.item.listener;

import cn.hutool.json.JSONUtil;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.constants.MQConstants;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ESUpdateMessageListener {
    private final IItemService itemService;
    private RestHighLevelClient client;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.ITEM_UPDATE_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.ITEM_EXCHANGE_NAME,type = ExchangeTypes.DIRECT,delayed = "true"),
            key = {MQConstants.ITEM_UPDATE_KEY})
    )
    public void updateES(Long itemId) throws IOException {
        //2.转化为ItemDoc格式,存入
        Item item = itemService.getById(itemId);
        ItemDoc itemDoc = BeanUtils.copyProperties(item, ItemDoc.class);
        //3.存入ES
        setUp();
        //3.1创建request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //3.2准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //3.3发送请求
        client.index(request, RequestOptions.DEFAULT);
        tearDown();
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.ITEM_UPDATE_STATUS_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.ITEM_EXCHANGE_NAME,type = ExchangeTypes.DIRECT,delayed = "true"),
            key = {MQConstants.ITEM_STATUS_UPDATE_KEY})
    )
    public void updateItemStatus2ES(Item item) throws IOException {
        //1.判断item的Status
        //1.1为1的话，上架，其他的话，下架
        Integer status = item.getStatus();
        setUp();
        if (status==1){
            //2.取出item对象
            Item itemUpdated = itemService.getById(item.getId());
            ItemDoc itemDoc = BeanUtils.copyProperties(itemUpdated, ItemDoc.class);
            //2.1创建request对象
            IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
            //2.2准备请求参数
            request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
            //2.3发送请求
            client.index(request, RequestOptions.DEFAULT);
        } else {
            //2.1创建request对象
            DeleteRequest request = new DeleteRequest("items",item.getId().toString());
            //2.2发送请求
            client.delete(request, RequestOptions.DEFAULT);
        }
        tearDown();
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.ITEM_UPDATE_ALL_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.ITEM_EXCHANGE_NAME,type = ExchangeTypes.DIRECT,delayed = "true"),
            key = {MQConstants.ITEM_ALL_UPDATE_KEY})
    )
    public void updateItem2ES(Item item) throws IOException {
        //1.转化为ItemDoc格式,存入
        ItemDoc itemDoc = BeanUtils.copyProperties(item, ItemDoc.class);
        //2.存入ES
        setUp();
        //2.1创建request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2.2准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //2.3发送请求
        client.index(request, RequestOptions.DEFAULT);
        tearDown();
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.ITEM_DELETE_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.ITEM_EXCHANGE_NAME,type = ExchangeTypes.DIRECT,delayed = "true"),
            key = {MQConstants.ITEM_DELETE_KEY})
    )
    public void deleteItemFromES(Long itemId) throws IOException {
        //1.从ES中删除
        setUp();
        //2.1创建request对象
        DeleteRequest request = new DeleteRequest("items",itemId.toString());
        //2.2发送请求
        client.delete(request, RequestOptions.DEFAULT);
        tearDown();
    }

    private void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.40.136:9200")
        ));
    }

    private void tearDown() throws IOException {
        if (client!=null){
            client.close();
        }
    }
}
