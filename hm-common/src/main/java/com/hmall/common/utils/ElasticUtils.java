package com.hmall.common.utils;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticUtils {

    private static RestHighLevelClient client;

    public static RestHighLevelClient getClient() {
//        RestHighLevelClient client;
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.175.129:9200")
        ));
        return client;
    }

}
