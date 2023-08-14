package com.nonononoki.alovoa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

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

    @Value("${spring.datasource.username}")
    private String dsUsername;

    @Value("${spring.datasource.password}")
    private String dsPassword;


    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(dsDriver);
        dataSourceBuilder.url(dsUrl);
        dataSourceBuilder.username(dsUsername);
        dataSourceBuilder.password(dsPassword);
        return dataSourceBuilder.build();
    }
}
