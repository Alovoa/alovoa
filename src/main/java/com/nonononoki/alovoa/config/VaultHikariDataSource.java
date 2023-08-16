package com.nonononoki.alovoa.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultHikariDataSource extends HikariDataSource {
    private static final Logger logger = LoggerFactory.getLogger(VaultHikariDataSource.class);

    private final MysqlCredentials mysqlCredentials = MysqlCredentials.getInstance();

    @Override
    public String getPassword() {
        return mysqlCredentials.getPassword();
    }

    @Override
    public String getUsername() {
        String username = mysqlCredentials.getUsername();
        logger.debug(String.format("Using username %s", username));
        return username;
    }

    public MysqlCredentials getMysqlCredentials() {
        return mysqlCredentials;
    }
}
