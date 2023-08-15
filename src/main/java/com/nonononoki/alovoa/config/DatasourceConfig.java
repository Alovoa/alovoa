package com.nonononoki.alovoa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/*
** Step (1): Does the same things as Spring Boot does w/o that class
 */
@ConfigurationProperties(prefix = "spring.datasource")
@Configuration
public class DatasourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatasourceConfig.class);

    private static URI vaultUrl;

    @Value("${spring.datasource.url}")
    private String dsUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String dsDriver;

    @Value("${spring.cloud.vault.token}")
    private String vaultToken;

    @Value("${spring.cloud.vault.uri}")
    private void setVaultUrl(String s) throws URISyntaxException {
        vaultUrl = new URI(s);
    }

    private MysqlCredentials mysqlCredentials;

    @Bean
    public DataSource getDataSource() {
        try {
            loadCredentials();
        } catch (URISyntaxException e) {
            logger.error(String.format("getDataSource failed: %s", e.getMessage()));
        }

        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(dsDriver);
        dataSourceBuilder.url(dsUrl);
        dataSourceBuilder.username(mysqlCredentials.username);
        dataSourceBuilder.password(mysqlCredentials.password);

        return dataSourceBuilder.build();
    }

    private void loadCredentials() throws URISyntaxException {
        VaultService vaultService = new VaultService(vaultUrl, vaultToken);
        mysqlCredentials = vaultService.getMysqlCredentials();
        logger.debug(String.format("Updated DataSource credentials: %s", mysqlCredentials.username));
    }

    @Scheduled(fixedDelay = 300000)
    private void updateCredentials() {
        try {
            loadCredentials();
            // ToDo: Refresh DataSource
            logger.info(String.format("Scheduler updateCredentials got username %s", mysqlCredentials.username));
        } catch (URISyntaxException e) {
            logger.error(String.format("Scheduler updateCredentials failed: %s", e.getMessage()));
        }
    }

}
