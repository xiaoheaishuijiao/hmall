package com.hmall.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.dto.CateAndBrand;
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
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.WeightBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
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
    private BoolQueryBuilder queryBuilder;

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
        queryBuilder = QueryBuilders.boolQuery();
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

        //广告加权值,原来的分数上面乘上100倍
        FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = {new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                QueryBuilders.termQuery("isAD", true),
                ScoreFunctionBuilders.weightFactorFunction(1000))};
        request.source()
                .query(QueryBuilders.functionScoreQuery(queryBuilder,filterFunctionBuilders).boostMode(CombineFunction.SUM))
                .from((query.getPageNo()-1)* query.getPageSize())
                .size(query.getPageSize());
        //要按照评分来排序
//                .sort(sortBy, query.getIsAsc()? SortOrder.ASC:SortOrder.DESC);
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析
        List<ItemDTO> itemDTOS = parseResponseResult(response);
        // 分页查询
        Page<ItemDTO> page=new Page<>(query.getPageNo(), query.getPageSize(),itemDTOS.size());
        System.out.println("no:"+query.getPageNo()+" size:"+query.getPageSize());
        //5.关闭ES
        tearDown();
        // 封装并返回PageDTO.of(result, ItemDTO.class)
        return PageDTO.of(page, itemDTOS);
    }

    /**
     * 根据id来查询商品
     *
     * @param id 商品id值
     * @return 商品DTO
     */
    @Override
    public ItemDTO listByIdByES(Long id) throws IOException {
        //1.开启ES连接
        setUp();
        //2.创建request
        SearchRequest request = new SearchRequest("items");
        //3.构建DSL语句
        request.source().query(QueryBuilders.termQuery("id",id));
        //4.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //5.解析
        List<ItemDTO> itemDTOS = parseResponseResult(response);
        //6.关闭ES
        tearDown();
        return itemDTOS.get(0);
    }

    /**
     * 根据ids来批量查询商品
     *
     * @param ids 商品的ids
     * @return 商品DTO
     */
    @Override
    public List<ItemDTO> listByIdsByES(List<Long> ids) throws IOException {
        //1.开启ES连接
        setUp();
        //1.1创建ItemDTOs
        List<ItemDTO> itemDTOReturn = new ArrayList<>();
        //2.创建request
        for (Long id : ids){
            SearchRequest request = new SearchRequest("items");
            //3.构建DSL语句
            request.source().query(QueryBuilders.termQuery("id",id));
            //4.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //5.解析
            List<ItemDTO> itemDTOS = parseResponseResult(response);
            itemDTOReturn.addAll(itemDTOS);
        }
        //6.关闭ES
        tearDown();
        //7.返回
        return itemDTOReturn;
    }

    @Override
    public CateAndBrand searchFilters(ItemPageQuery query) throws IOException {
        //1.打开ES
        setUp();
        //2.根据key或brand或category聚合搜索
        SearchRequest request = new SearchRequest("items");
        //2.1.构建DSL语句
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
        request.source().query(queryBuilder);
        //2.2聚合
        request.source().aggregation(AggregationBuilders
                .terms("brand_agg")
                .field("brand")
                .size(8));
        request.source().aggregation(AggregationBuilders
                .terms("category_agg")
                .field("category")
                .size(8));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析聚合分类
        return parseResponseAggResult(response);
    }

    private CateAndBrand parseResponseAggResult(SearchResponse response) {
        //1.新建一个CateAndBrand类
        CateAndBrand cateAndBrand = new CateAndBrand();
        List<String> categories = new ArrayList<>();
        List<String> brands = new ArrayList<>();
        //2.聚合查询
        // 2.1取出Category分类
        Terms categoryTerms = response.getAggregations().get("category_agg");
        List<? extends Terms.Bucket> CategoryBuckets = categoryTerms.getBuckets();
        for (Terms.Bucket bucket:CategoryBuckets){
            categories.add(bucket.getKeyAsString());
        }
        //2.2取出Brand分类
        Terms brandTerms = response.getAggregations().get("brand_agg");
        List<? extends Terms.Bucket> BrandBucket = brandTerms.getBuckets();
        for (Terms.Bucket bucket:BrandBucket){
            brands.add(bucket.getKeyAsString());
        }
        //3.分装
        cateAndBrand.setCategory(categories);
        cateAndBrand.setBrand(brands);
        System.out.println("categoryAndBrand:"+cateAndBrand);
        return cateAndBrand;
    }

    /**
     * 解析es中得到的的结果，封装成ItemDTO序列返还
     * @param response ES返回的结果
     * @return List<ItemDTO>
     */
    public List<ItemDTO> parseResponseResult(SearchResponse response) {
        SearchHits searchHits = response.getHits();
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

    private static Long getSearchTotal(SearchResponse response) {
        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1总条数返回
        return searchHits.getTotalHits().value;
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
