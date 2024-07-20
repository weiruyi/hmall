package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;


@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocunmentTest {
    private RestHighLevelClient client;

    @Autowired
    private IItemService itemService;

    @Test
    void testAddDocument() throws IOException {
        // 1.根据id查询商品数据
        Item item = itemService.getById(100002644680L);
        // 2.转换为文档类型
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        // 3.将ItemDTO转json
        String doc = JSONUtil.toJsonStr(itemDoc);

        // 1.准备Request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        // 2.准备Json文档
        request.source(doc, XContentType.JSON);
        // 3.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkDoc() throws IOException {
        int pageNo = 1, pageSize = 500;
        while (true){
            //1、准备文档数据
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo, pageSize));
            List<Item> records = page.getRecords();
            if (records == null || records.isEmpty()){
                return;
            }

            //2、准备request
            BulkRequest request = new BulkRequest();
            //3、准备请求参数
            for(Item record : records){
                request.add(new IndexRequest("items")
                        .id(record.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(record, ItemDoc.class)), XContentType.JSON));
            }
            //4、发送数据
            client.bulk(request, RequestOptions.DEFAULT);
            //5、翻页
            pageNo++;
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
