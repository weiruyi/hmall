package com.hmall.item.es;

import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.ItemDoc;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.IOException;
import java.util.List;


//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {
    private RestHighLevelClient client;


    @Test
    void testMatchAll() throws IOException {
        //1、创建request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        request.source()
                .query(QueryBuilders.matchAllQuery());

//
//        request.source()
//                .query(QueryBuilders.boolQuery()
//                        .must(QueryBuilders.matchQuery("name", "脱脂牛奶"))
//                        .filter(QueryBuilders.termQuery("brand", "德亚"))
//                        .filter(QueryBuilders.rangeQuery("price").lt(30000)))
//                .sort("price", SortOrder.DESC)
//                .from(1)
//                .size(5)
//                .highlighter(SearchSourceBuilder.highlight().field("name"));


        //3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        System.out.println("response: " + response);

        //4、解析结果
        SearchHits searchHits = response.getHits();
        //4.1、总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("total: " + total);
        //4.2、命中的数据
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //4.2.1 获取source结果
            String json = hit.getSourceAsString();
            //4.2.2 转为ItemDoc
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
            System.out.println("ItemDoc:" + itemDoc);
        }
    }


    @Test
    void testAgg() throws IOException {
        //1、创建request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        request.source()
                .query(QueryBuilders.termQuery("category", "手机"))
                .size(0);
        request.source().aggregation(
                AggregationBuilders.terms("brand_agg")
                        .field("brand")
                        .size(5)
        );


        //3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        System.out.println("response: " + response);

        //4、解析结果
        Aggregations aggregations = response.getAggregations();
        Terms brandTerms = aggregations.get("brand_agg");

        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            Object key = bucket.getKey();
            long docCount = bucket.getDocCount();
            System.out.println("key: " + key + ", docCount: " + docCount);
        }

    }



    @BeforeEach
    void setUp(){
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.175.129:9200")
        ));
    }


    @AfterEach
    void tearDown() throws IOException {
        if(client != null){
            client.close();
        }
    }

}
