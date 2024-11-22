package com.hmall.api.client;

import com.hmall.api.client.fallback.SearchClientFallbackFactory;
import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.ItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(value = "search-service",configuration = DefaultFeignConfig.class,fallbackFactory = SearchClientFallbackFactory.class)
public interface SearchClient {

    @GetMapping("/search/{id}")
    ItemDTO searchById(@PathVariable("id") Long id);

    @GetMapping("/search")
    List<ItemDTO> searchByIds(@RequestParam("ids") Collection<Long> ids);
}
