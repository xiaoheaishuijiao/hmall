package com.hmall.api.client.fallback;

import com.hmall.api.client.SearchClient;
import com.hmall.api.dto.ItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Slf4j
public class SearchClientFallbackFactory implements FallbackFactory<SearchClient> {

    @Override
    public SearchClient create(Throwable cause) {
        return new SearchClient() {

            @Override
            public ItemDTO searchById(Long id){
                log.error("查询商品失败",cause);
                return null;
            }

            @Override
            public List<ItemDTO> searchByIds(Collection<Long> ids){
                log.error("批量查询商品失败",cause);
                return Collections.emptyList();
            }
        };
    }
}
