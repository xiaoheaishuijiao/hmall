package com.hmall.cart.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;

@Component
@ConfigurationProperties(prefix = "hm.cart")
@Data
public class CartProperties {
    private Integer maxItems;
}
