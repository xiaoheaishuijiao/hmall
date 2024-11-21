package com.hmall.api.client;

import com.hmall.api.client.fallback.UserClientFallbackFactory;
import com.hmall.api.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

@FeignClient(value = "user-service",configuration = DefaultFeignConfig.class,fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @PutMapping("users/money/deduct")
    void deductMoney(@RequestParam("pw") String pw,@RequestParam("amount") Integer amount);
}
