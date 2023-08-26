package com.nonononoki.alovoa.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.protocol.ProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisConfiguration {
    private final static Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    private boolean redisEnabled;

    @Value("${app.redis.enabled}")
    private void enableRedis(String s) {redisEnabled = Boolean.parseBoolean(s); }

    @Value("${spring.data.redis.host:#{null}}")
    private String host;

    @Value("#{'${spring.data.redis.cluster.nodes}'.split(',')}")
    private List<String> redisCluster;

    @Value("${spring.redis.port:0}")
    private int port;


    @Value("${spring.data.redis.username:#{null}}")
    private String redisUsername;

    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;

    @Value("${spring.ssl.bundle.jks.redisbundle.truststore.location:#{null}}")
    private String caCertificateFile;


    @Value("${spring.ssl.bundle.jks.redisbundle.truststore.password}")
    private String trustStorePassword;

    @Value("${spring.ssl.bundle.jks.redisbundle.keystore.location:#{null}}")
    private String clientCertificateFile;

    @Value("${spring.ssl.bundle.jks.redisbundle.keystore.password:#{null}}")
    private String clientCertificatePassword;

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    private RedisTemplate<String, byte[]> redisTemplate;


    private RedisConnectionFactory redisConnectionFactory() throws IOException {
        RedisStandaloneConfiguration redisStandaloneConfiguration = null;
        RedisClusterConfiguration redisClusterConfiguration = null;
        if (host!=null) {
            redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setHostName(host);
            redisStandaloneConfiguration.setPort(port);
            redisStandaloneConfiguration.setUsername(redisUsername);
            redisStandaloneConfiguration.setPassword(redisPassword);
        } else if (redisCluster!=null && !redisCluster.isEmpty()) {
            redisClusterConfiguration = new RedisClusterConfiguration();
            List<RedisNode> redisNodeList = new ArrayList<>();
            redisCluster.forEach((v) -> {
                HostAndPort hostAndPort = HostAndPort.parse(v);
                redisNodeList.add(new RedisNode(hostAndPort.getHostText(), hostAndPort.getPort()));
            });
            redisClusterConfiguration.setClusterNodes(redisNodeList);
            redisClusterConfiguration.setUsername(redisUsername);
            redisClusterConfiguration.setPassword(redisPassword);
        } else {
            throw new IOException("Either redisHost or redisCluster must be set");
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder lettuceClientConfigurationBuilder =
                LettuceClientConfiguration.builder();

        if (caCertificateFile != null) {
            SslOptions sslOptions;
            if (clientCertificateFile != null) {
                sslOptions = SslOptions.builder()
                        .truststore(resourceLoader.getResource(caCertificateFile).getFile(), trustStorePassword)
                        .keystore(
                                resourceLoader.getResource(clientCertificateFile).getFile(),
                                clientCertificatePassword.toCharArray()
                        )
                        .build();
                logger.info("Loaded keystore and truststore");
            } else {
                sslOptions = SslOptions.builder()
                        .truststore(resourceLoader.getResource(caCertificateFile).getFile(), trustStorePassword)
                        .build();
            }
            ClientOptions clientOptions = ClientOptions
                    .builder()
                    .sslOptions(sslOptions)
                    .protocolVersion(ProtocolVersion.RESP3)
                    .build();

            lettuceClientConfigurationBuilder
                    .clientOptions(clientOptions)
                    .useSsl();
        }

        LettuceClientConfiguration lettuceClientConfiguration = lettuceClientConfigurationBuilder.build();
        return getLettuceConnectionFactory(redisStandaloneConfiguration, lettuceClientConfiguration, redisClusterConfiguration);
    }

    private static LettuceConnectionFactory getLettuceConnectionFactory(
            RedisStandaloneConfiguration redisStandaloneConfiguration,
            LettuceClientConfiguration lettuceClientConfiguration,
            RedisClusterConfiguration redisClusterConfiguration
    ) throws IOException {
        LettuceConnectionFactory lettuceConnectionFactory;
        if (redisStandaloneConfiguration !=null) {
            lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, lettuceClientConfiguration);
        } else if (redisClusterConfiguration !=null) {
            lettuceConnectionFactory = new LettuceConnectionFactory(redisClusterConfiguration, lettuceClientConfiguration);
        } else {
            throw new IOException("Strange: Either redisHost or redisCluster must be set");
        }

        lettuceConnectionFactory.afterPropertiesSet();
        return lettuceConnectionFactory;
    }

    @Bean
    void initRedis() {
        if (!redisEnabled) {
            logger.info("Redis is disabled by configuration, so we do nothing here");
            return;
        }
        if (host==null && redisCluster==null) {
            logger.info("No Redis host or cluster is set");
            return;
        }
        logger.info(String.format("Initializing Redis connection with username %s", redisUsername));
        try {
            RedisConnectionFactory redisConnectionFactory = redisConnectionFactory();
            redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
            redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Object.class));
            redisTemplate.afterPropertiesSet();
        } catch (IOException e) {
            logger.warn(String.format("initRedis: %s", e.getMessage()));
        }
    }

    public void save(String key, byte[] blob) {
        redisTemplate.opsForValue().set(key, blob);
    }

    public byte[] findById(String key) {
        return redisTemplate.opsForValue().get(key);
    }

}
