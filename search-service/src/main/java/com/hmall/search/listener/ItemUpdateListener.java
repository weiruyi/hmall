package com.hmall.search.listener;


import cn.hutool.json.JSONUtil;
import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSONObject;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.es.ElasticSearch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemUpdateListener {

    private final ElasticSearch elasticSearch;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.item.index.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct"),
            key = "item.index"
    ))
    public void listenUpdate(ItemDTO item){
        log.info("New itemStr:{}", item);
//        ItemDTO item = JSONObject.parseObject(itemStr, ItemDTO.class);
        elasticSearch.add(item);
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.item.delete.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct"),
            key = "item.delete"
    ))
    public void listenDelete(Long id){
        log.info("Delete id:{}", id);
        elasticSearch.rm(id);

    }

}
