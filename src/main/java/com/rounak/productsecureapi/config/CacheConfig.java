package com.rounak.productsecureapi.config;

import com.rounak.productsecureapi.dto.product.ProductResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCT_BY_ID = "productById";
    public static final String PRODUCT_LIST = "productList";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues();

        JsonMapper mapper = JsonMapper.builder().build();
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, ProductResponse.class);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withCacheConfiguration(PRODUCT_BY_ID,
                        withSerializer(base, new JacksonJsonRedisSerializer<>(ProductResponse.class)))
                .withCacheConfiguration(PRODUCT_LIST,
                        withSerializer(base, new JacksonJsonRedisSerializer<List<ProductResponse>>(listType)))
                .disableCreateOnMissingCache()
                .build();
    }

    private RedisCacheConfiguration withSerializer(RedisCacheConfiguration base, RedisSerializer<?> serializer) {
        return base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
