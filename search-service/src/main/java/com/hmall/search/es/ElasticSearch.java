package com.hmall.search.es;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.ElasticUtils;
import com.hmall.search.domain.dto.PageDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

@Component
public class ElasticSearch {

    private  final  RestHighLevelClient client = ElasticUtils.getClient();

    /**
     * 获取搜索请求
     * @param query
     * @return
     */
    public SearchRequest getSearchRequest(ItemPageQuery query) {
        //1、获取request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        //2.1 关键词搜索
        if(query.getKey() != null && query.getKey() != ""){
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }else {
            bool.must(QueryBuilders.matchAllQuery());
        }
        //2.2 品牌名过滤
        if(query.getBrand()!= null && query.getBrand() != ""){
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        //2.3 种类过滤
        if(query.getCategory()!= null && query.getCategory() != ""){
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        // 2.4 价格过滤
        RangeQueryBuilder rangedQuery = QueryBuilders.rangeQuery("price");
        if(query.getMaxPrice() != null && query.getMaxPrice() != 0){
            rangedQuery.lte(query.getMaxPrice());
        }
        if(query.getMinPrice() != null && query.getMinPrice() != 0){
            rangedQuery.gte(query.getMinPrice());
        }
        bool.filter(rangedQuery);
        //3、排序和分页
        request.source().query(bool);
        request.source().sort("updateTime", SortOrder.DESC);
        request.source().from((query.getPageNo()-1) * query.getPageSize()).size(query.getPageSize());
        return request;
    }

    /**
     * 广告优先
     * @param query
     * @return
     */
    public SearchRequest getSearchRequestAd(ItemPageQuery query) {
        //1、获取request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        //2.1 关键词搜索
        if(query.getKey() != null && query.getKey() != ""){
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }else {
            bool.must(QueryBuilders.matchAllQuery());
        }
        //2.2 品牌名过滤
        if(query.getBrand()!= null && query.getBrand() != ""){
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        //2.3 种类过滤
        if(query.getCategory()!= null && query.getCategory() != ""){
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        // 2.4 价格过滤
        RangeQueryBuilder rangedQuery = QueryBuilders.rangeQuery("price");
        if(query.getMaxPrice() != null && query.getMaxPrice() != 0){
            rangedQuery.lte(query.getMaxPrice());
        }
        if(query.getMinPrice() != null && query.getMinPrice() != 0){
            rangedQuery.gte(query.getMinPrice());
        }
        bool.filter(rangedQuery);

        FunctionScoreQueryBuilder scoreQueryBuilder = QueryBuilders.functionScoreQuery(
                // 原始查询，相关性算分
                bool,
                // function score的数组
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        // 其中的一个function score元素
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                // 过滤条件
                                QueryBuilders.termQuery("isAD", true),
                                // 算分函数
                                ScoreFunctionBuilders.weightFactorFunction(10))
                });

        //3、排序和分页
//        request.source().query(bool);
        request.source().query(scoreQueryBuilder);
        request.source().sort("_score", SortOrder.DESC);
        request.source().sort("updateTime", SortOrder.DESC);
        request.source().from((query.getPageNo()-1) * query.getPageSize()).size(query.getPageSize());
        return request;
    }

    /**
     * 搜索
     * @param query
     * @return
     */
    public PageDTO<ItemDTO> search(ItemPageQuery query){

//        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
//                HttpHost.create("http://192.168.175.129:9200")
//        ));

        SearchRequest request = getSearchRequestAd(query);
//        SearchRequest request = getSearchRequest(query);
        //4、发送请求
        SearchResponse response = null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
            //5、解析请求
            PageDTO<ItemDTO> pageDTO = handleResponse(response);
            return pageDTO;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析搜索响应
     * @param response
     * @return
     */
    private PageDTO<ItemDTO> handleResponse(SearchResponse response) {
        PageDTO<ItemDTO> pageDTO = new PageDTO<>();

        SearchHits searchHits = response.getHits();
        // 1.获取总条数
        long total = searchHits.getTotalHits().value;
        pageDTO.setTotal(total);

        // 2.遍历结果数组
        List<ItemDTO> list = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        if(hits == null || hits.length == 0){
            return pageDTO;
        }
        for (SearchHit hit : hits) {
            // 3.得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 4.反序列化并打印
            ItemDTO item = JSONUtil.toBean(source, ItemDTO.class);
            list.add(item);
        }
        pageDTO.setList(list);
        Long pages = total / list.size();
        pageDTO.setPages(pages);
        return pageDTO;

    }


    /**
     * 更新es，新增
     * @param item
     */
    public void add(ItemDTO item) {
        //1、转为文档数据类型
        ItemDoc itemDoc = BeanUtils.copyProperties(item, ItemDoc.class);
        //2、准备request
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //3、准备json文档
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //4、发送请求
        try {
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除
     * @param id
     */
    public void rm(Long id) {
        DeleteRequest request = new DeleteRequest("items", id.toString());
        try {
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param query
     * @return
     */
    public Map filters(ItemPageQuery query) {
        Map<String, List<String>> map = new HashMap<>();
        List<String> brandList = new ArrayList<>();
        List<String> categoryList = new ArrayList<>();

        if(StrUtil.isBlank(query.getKey()) && StrUtil.isBlank(query.getBrand()) && StrUtil.isBlank(query.getCategory())){
            //制造假数据
            Collections.addAll(brandList, "希捷","小米","华为", "oppo", "尤妮佳","莎米特", "意尔康", "新秀丽" , "Apple", "锤子");
            Collections.addAll(categoryList,"手机", "曲面电视", "拉杆箱", "休闲鞋", "硬盘", "真皮包");

            map.put("brand", brandList);
            map.put("category", categoryList);
            return map;
        }

        //1、获取搜索请求
        SearchRequest request = getSearchRequest(query);

        //2、聚合参数
        //2.1 种类
        request.source().aggregation(
                AggregationBuilders.terms("category_agg").field("category").size(10)
        );
        //2.2 品牌
        request.source().aggregation(
                AggregationBuilders.terms("brand_agg").field("brand").size(10)
        );

        //3、发送请求
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4、解析结果
            Aggregations aggregations = response.getAggregations();
            //4.1 获取品牌聚合
            Terms brandTerms = aggregations.get("brand_agg");
            // 4.2 获取种类聚合
            Terms categoryTerms = aggregations.get("category_agg");

            // 4.3 获取buckets
            List<? extends Terms.Bucket> brandTermsBuckets = brandTerms.getBuckets();
            List<? extends Terms.Bucket> categoryTermsBuckets = categoryTerms.getBuckets();
            // 4.4 遍历获取每一个bucket
            for (Terms.Bucket brandTermsBucket : brandTermsBuckets) {
                brandList.add(brandTermsBucket.getKeyAsString());
            }
            for (Terms.Bucket categoryTermsBucket : categoryTermsBuckets) {
                categoryList.add(categoryTermsBucket.getKeyAsString());
            }

            map.put("brand", brandList);
            map.put("category", categoryList);

            return map;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
