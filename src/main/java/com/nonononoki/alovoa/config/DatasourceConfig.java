package com.nonononoki.alovoa.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/*
** Step (1): Does the same things as Spring Boot does w/o that class
 */
@ConfigurationProperties(prefix = "spring.datasource")
@Configuration
public class DatasourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatasourceConfig.class);

    @Value("${spring.datasource.url}")
    private String dsUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String dsDriver;

    private URI vaultUri;
    @Value("${spring.cloud.vault.uri}")
    private void setVaultUri(String newVaultUri) throws URISyntaxException {
        vaultUri = new URI(newVaultUri);
    };

    @Value("${spring.cloud.vault.token}")
    private String vaultToken;

    @Value("${spring.cloud.vault.templatepath}")
    private String vaultTemplatePath;

    @Bean
    public VaultHikariDataSource getHikariDataSource() {
        Properties properties = new Properties();
        properties.setProperty("cachePrepStmts" , "true");
        properties.setProperty("prepStmtCacheSize" , "250" );
        properties.setProperty("prepStmtCacheSqlLimit" , "2048" );

        VaultHikariDataSource vaultHikariDataSource = new VaultHikariDataSource();
        vaultHikariDataSource.setDriverClassName(dsDriver);
        vaultHikariDataSource.setJdbcUrl(dsUrl);

        vaultHikariDataSource.getMysqlCredentials().setVaultUrl(vaultUri);
        vaultHikariDataSource.getMysqlCredentials().setVaultToken(vaultToken);
        vaultHikariDataSource.getMysqlCredentials().setVaultTemplatePath(vaultTemplatePath);
        vaultHikariDataSource.getMysqlCredentials().update();

        vaultHikariDataSource.setDataSourceProperties(properties);
        return vaultHikariDataSource;
    }

    @PostConstruct
    private void postConstruct() {
        logger.info("postConstruct called");
    }

}
