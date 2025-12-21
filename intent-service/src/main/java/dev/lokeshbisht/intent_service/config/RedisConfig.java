package dev.lokeshbisht.intent_service.config;

import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.*;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.timeout}")
    private long timeout;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(timeout))
            .shutdownTimeout(Duration.ZERO)
            .build();

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(port);
        redisConfig.setDatabase(database);  // <--- logical database index

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * Key → String
     * Value → String (Idempotency keys, simple flags)
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory);
    }

    /**
     * Key → String
     * Value → JSON Object (for storing complex objects)
     */
    @Bean
    public ReactiveRedisTemplate<String, CreateIntentResponse> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        JacksonJsonRedisSerializer<CreateIntentResponse> valueSerializer = new JacksonJsonRedisSerializer<>(CreateIntentResponse.class);


        RedisSerializationContext<String, CreateIntentResponse> context = RedisSerializationContext.<String, CreateIntentResponse>newSerializationContext(keySerializer)
            .value(valueSerializer)
            .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
