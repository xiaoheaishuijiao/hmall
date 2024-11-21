package com.hmall.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.mapper.SearchMapper;
import com.hmall.search.service.ISearchService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl extends ServiceImpl<SearchMapper, Item> implements ISearchService {

    private RestHighLevelClient client;

    /**
     * 通过es来实现分页查询
     * @param query 查参与查询的参数，可能为空
     * @return PageDTO<ItemDTO>
     * @throws IOException 连接关闭异常
     */
    @Override
    public PageDTO<ItemDTO> listByES(ItemPageQuery query) throws IOException {
        //开启连接ElasticSearch
        setUp();
        //1.获取query里的参数(关键词(null)，品牌(null)，分类(null)，状态(不用判断，因为es里的数据没有不正常的)，最大/小价格(null)，)
        String sortBy = query.getSortBy();
        if (StrUtil.isBlank(sortBy)){
            sortBy="updateTime";
        }
        //2.创建request对象
        SearchRequest request = new SearchRequest("items");
        //3.构建DSL语句
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (!StrUtil.isBlank(query.getKey())){
            queryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (!StrUtil.isBlank(query.getBrand())){
            queryBuilder.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (!StrUtil.isBlank(query.getCategory())){
            queryBuilder.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (query.getMaxPrice()!=null&&query.getMinPrice()!=null){
            queryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }
        request.source()
                .query(queryBuilder)
                .from((query.getPageNo()-1)* query.getPageSize())
                .size(query.getPageSize())
                .sort(sortBy, query.getIsAsc()? SortOrder.ASC:SortOrder.DESC);
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析
        List<ItemDTO> itemDTOS = parseResponseResult(response);
        // 分页查询
        Page<ItemDTO> page=new Page<>(query.getPageNo(), query.getPageSize());
        tearDown();
        // 封装并返回PageDTO.of(result, ItemDTO.class)
        return PageDTO.of(page, itemDTOS);
    }

    /**
     * 解析es中得到的的结果，封装成ItemDTO序列返还
     * @param response ES返回的结果
     * @return List<ItemDTO>
     */
    public List<ItemDTO> parseResponseResult(SearchResponse response) {
        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1总条数
        long total = searchHits.getTotalHits().value;
//        System.out.println("total:"+total);
        //4.2命中的数据
        SearchHit[] hits = searchHits.getHits();
        List<ItemDTO> itemDTOs = new ArrayList<>();
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
            ItemDTO itemDTO = BeanUtil.copyProperties(itemDoc, ItemDTO.class);
            itemDTOs.add(itemDTO);
        }
        return itemDTOs;
    }

    private void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.40.136:9200")
        ));
    }

    private void testConnection(){
        System.out.println("client="+client);
    }

    private void tearDown() throws IOException {
        if (client!=null){
            client.close();
        }
    }

}
