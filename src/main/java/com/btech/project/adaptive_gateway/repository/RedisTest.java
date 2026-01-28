package com.btech.project.adaptive_gateway.repository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RedisTest implements CommandLineRunner {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisTest(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        // We are using Reactive programming (Mono/Flux) since this is a Reactive Gateway
        redisTemplate.opsForValue().set("connection-status", "Redis is officially connected!")
                .then(redisTemplate.opsForValue().get("connection-status"))
                .subscribe(value -> log.info("REDIS CHECK >> {}", value));
    }
}