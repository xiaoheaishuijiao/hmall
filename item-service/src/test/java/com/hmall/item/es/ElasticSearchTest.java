package com.hmall.item.es;

import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {

    @Autowired
    private IItemService itemService;

    private RestHighLevelClient client;

    @Test
    void testConnection(){
        System.out.println("client="+client);
    }

    @Test
    void testSearch() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("items");
        //2.构造DSL条件语句
        request.source()
                .query(QueryBuilders.matchAllQuery());
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testBoolSearch() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("items");
        //2.构造DSL条件语句
        request.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name","脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand","德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lte(30000))
                );
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testSortAndPage() throws IOException {
        int pageNo = 1,pageSize = 5;
        //1.创建request
        SearchRequest request = new SearchRequest("items");
        //2.构建DSL条件语句,分页和排序
        request.source().from((pageNo-1)*pageSize)
                .size(pageSize)
                .sort("sold", SortOrder.DESC)
                .sort("price", SortOrder.ASC);
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析请求
        parseResponseResult(response);
    }

    @Test
    void testHighlightTag() throws IOException {
        //1.创建request
        SearchRequest request = new SearchRequest("items");
        //2.构建DSL条件语句,分页和排序
        request.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        //2.1 高亮条件
        request.source().highlighter(SearchSourceBuilder.highlight()
                .field("name")
                .preTags("<em>")
                .postTags("</em>"));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析请求
        parseResponseResult(response);
    }

    @Test
    void testAggregation() throws IOException {
        String aggName = "brandAgg";
        //1.创建request
        SearchRequest request = new SearchRequest("items");
        //2.构建DSL条件语句,分页和排序
        request.source().size(0);
        //2.1 高亮条件
        request.source().aggregation(AggregationBuilders
                .terms(aggName)
                .field("brand")
                .size(10));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析请求
        Aggregations aggregations = response.getAggregations();
        //4.1更具聚合名称获取所对应的聚合
        Terms brandTerms = aggregations.get(aggName);
        //4.2获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        //4.3遍历每个桶，获取结果
        for (Terms.Bucket bucket : buckets) {
            System.out.println("brand:"+bucket.getKeyAsString());
            System.out.println("size:"+bucket.getDocCount());
        }
    }

    private static void parseResponseResult(SearchResponse response) {
        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("total:"+total);
        //4.2命中的数据
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //4.2.1获取source结果
            String json = hit.getSourceAsString();
            //4.2.2转为itemDoc
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
            //4.3处理高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            //不为空处理
            if (highlightFields!=null&&!highlightFields.isEmpty()) {
                //获取高亮结果
                HighlightField name = highlightFields.get("name");
                String hfs = Arrays.stream(name.getFragments()).collect(Collectors.toList()).toString();
                //覆盖非高亮结果
                itemDoc.setName(hfs);
            }
            System.out.println("itemDoc:"+itemDoc);
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
