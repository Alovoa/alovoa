package com.nonononoki.alovoa.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultHikariDataSource extends HikariDataSource {
    private static final Logger logger = LoggerFactory.getLogger(VaultHikariDataSource.class);

    private final DatasourceCredentials datasourceCredentials = DatasourceCredentials.getInstance();

    @Override
    public String getPassword() {
        return datasourceCredentials.getPassword();
    }

    @Override
    public String getUsername() {
        String username = datasourceCredentials.getUsername();
        logger.debug(String.format("Using username %s", username));
        return username;
    }

    public DatasourceCredentials getMysqlCredentials() {
        return datasourceCredentials;
    }
}
