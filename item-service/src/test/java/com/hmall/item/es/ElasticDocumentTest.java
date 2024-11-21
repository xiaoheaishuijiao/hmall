package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocumentTest {

    @Autowired
    private IItemService itemService;

    private RestHighLevelClient client;

    @Test
    void testConnection(){
        System.out.println("client="+client);
    }

    @Test
    void testIndexDoc() throws IOException {
        //0.获取对象
        Item item = itemService.getById(317578L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);

        //1.创建request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2.准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc),XContentType.JSON);
        //3.发送请求
        client.index(request,RequestOptions.DEFAULT);
    }

    @Test
    void testUpdateDoc() throws IOException {
        //1.创建request对象
        UpdateRequest request = new UpdateRequest("items","317578");
        //2.准备请求参数
        request.doc(
                "price",29900
        );
        //3.发送请求
        client.update(request,RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDoc() throws IOException {
        //1.创建request对象
        DeleteRequest request = new DeleteRequest("items","317578");
        //2.发送请求
        client.delete(request,RequestOptions.DEFAULT);
    }

    @Test
    void testGetDoc() throws IOException {
        //1.创建request对象
        GetRequest request = new GetRequest("items","317578");
        //2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //3.解析相应内容
        String json = response.getSourceAsString();
        ItemDoc itemDoc = BeanUtil.copyProperties(JSONUtil.toJsonStr(json), ItemDoc.class);
        System.out.println("docs:"+itemDoc);
    }

    @Test
    void testBulkDoc() throws IOException {
        //0.获取对象
        int pageNo = 1;
        int pageSize = 500;
        while (true){
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo,pageSize));
            List<Item> items = page.getRecords();
            if (items==null||items.isEmpty()){
                return;
            }
            //1.创建request对象
            BulkRequest request = new BulkRequest();
            for (Item item:items){
                //2.准备请求参数
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item,ItemDoc.class)),XContentType.JSON));
            }
            //3.发送请求
            client.bulk(request,RequestOptions.DEFAULT);
            pageNo++;
        }

    }

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.40.136:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client!=null){
            client.close();
        }
    }
}
