package com.hmall.api.client.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        log.error("itemClient error:",cause);
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                return CollUtils.emptyList();
            }

            @Override
            public void deductStock(Collection<OrderDetailDTO> items) {
                throw new RuntimeException(cause);
            }

            @Override
            public void restoreStock(List<OrderDetailDTO> orderDetailDTOS) {

            }
        };
    }
}
