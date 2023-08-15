package com.nonononoki.alovoa.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URI;
import java.net.URISyntaxException;

/*
** Step (1): Does the same things as Spring Boot does w/o that class
 */
@ConfigurationProperties(prefix = "spring.datasource")
@Configuration
//@Component
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

    private RefreshEndpoint refreshEndpoint;

    @Autowired
    public void ConfigRefreshJob(final RefreshEndpoint refreshEndpoint) {
        this.refreshEndpoint = refreshEndpoint;
    }

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;


    @Bean
    public HikariDataSource getHikariDataSource() {
        try {
            loadCredentials();
        } catch (URISyntaxException e) {
            logger.error(String.format("getDataSource failed: %s", e.getMessage()));
        }
        config.setJdbcUrl(dsUrl);
        config.setUsername(mysqlCredentials.username);
        config.setPassword(mysqlCredentials.password);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        return new HikariDataSource(config);
    }

    private void loadCredentials() throws URISyntaxException {
        VaultService vaultService = new VaultService(vaultUrl, vaultToken);
        mysqlCredentials = vaultService.getMysqlCredentials();
        logger.debug(String.format("Updated DataSource credentials: %s", mysqlCredentials.username));
    }

    @PostConstruct
    private void postConstruct() {

    }

    @Scheduled(fixedDelay = 300000)
    private void updateCredentials() {
        //logger.info("Refreshing Configurations - {}", refreshEndpoint.refresh());
        try {
            loadCredentials();
            config.setUsername(mysqlCredentials.username);
            config.setPassword(mysqlCredentials.password);
            // ToDo: Refresh DataSource
            logger.info(String.format("Scheduler updateCredentials got username %s", mysqlCredentials.username));
        } catch (URISyntaxException e) {
            logger.error(String.format("Scheduler updateCredentials failed: %s", e.getMessage()));
        }
    }

}
