package com.nonononoki.alovoa.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestPropertySource("classpath:application-test.properties")
class RedisConfigurationTest {
    private final static Logger logger = LoggerFactory.getLogger(RedisConfigurationTest.class);

    private static RedisConfiguration redisConfiguration;

    @BeforeAll
    static void initRedis() {
        logger.info("initRedis called");
        redisConfiguration = new RedisConfiguration();
        assertNotNull(redisConfiguration);
        redisConfiguration.initRedis();
    }

    @Test
    void testData() throws NoSuchAlgorithmException {
        UUID testUUID = new UUID(SecureRandom.getInstanceStrong().nextLong(), SecureRandom.getInstanceStrong().nextLong());
        logger.info(String.format("Generated UUID: %s", testUUID));
        byte[] testData = "Hello Java Spring Boot world!".getBytes(StandardCharsets.UTF_8);

        final String key = "java";
        redisConfiguration.save(key, testData);

        byte[] valueByte = redisConfiguration.findById(key);
        String valueString = new String(valueByte);
        logger.info(String.format("Got value: %s", valueString));
    }

}
