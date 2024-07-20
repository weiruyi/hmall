package com.hmall.search.controller;


import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.dto.PageDTO;
import com.hmall.search.es.ElasticSearch;
import com.hmall.search.domain.query.ItemPageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticSearch elasticSearch;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) {
        // 分页查询
        PageDTO<ItemDTO> page = elasticSearch.search(query);
        // 封装并返回
        return page;
    }

    @ApiOperation("过滤条件聚合")
    @PostMapping("/filters")
    public Map filters(@RequestBody ItemPageQuery query) {
        Map map = elasticSearch.filters(query);
        return map;
    }
}
