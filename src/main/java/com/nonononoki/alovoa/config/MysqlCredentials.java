package com.nonononoki.alovoa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.net.URI;
import java.util.Map;

@Component
public class MysqlCredentials {
    private static final Logger logger = LoggerFactory.getLogger(MysqlCredentials.class);

    private static MysqlCredentials mysqlCredentials;
    private static String username;
    private static String password;

    private static URI vaultUrl;
    private static String vaultToken;
    private static String vaultTemplatePath;

    private MysqlCredentials() {}

    public static MysqlCredentials getInstance() {

        if (mysqlCredentials == null) {
            logger.info("Created new MysqlCredentials instance");
            mysqlCredentials = new MysqlCredentials();
        } else {
            logger.info("Using existing MysqlCredentials instance");
        }
        return mysqlCredentials;
    }

    public void setVaultUrl(URI newVaultUrl) {
        vaultUrl = newVaultUrl;
    }

    public void setVaultToken(String newVaultToken) {
        vaultToken = newVaultToken;
    }

    public void setVaultTemplatePath(String newVaultTemplatePath) {
        vaultTemplatePath = newVaultTemplatePath;
    }

    @Scheduled(fixedDelay = 300000)
    public void update() {
        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setHost(vaultUrl.getHost());
        vaultEndpoint.setScheme(vaultUrl.getScheme());
        vaultEndpoint.setPort(vaultUrl.getPort());
        VaultTemplate vaultTemplate = new VaultTemplate(
                vaultEndpoint,
                new TokenAuthentication(vaultToken)
        );

        VaultResponse vaultResponse = vaultTemplate.read(vaultTemplatePath);
        assert vaultResponse != null;
        Map<String, Object> vaultResponseData = vaultResponse.getData();
        assert vaultResponseData != null;
        username = vaultResponseData.get("username").toString();
        password = vaultResponseData.get("password").toString();
        logger.debug(String.format("MySQL Credentials updated with username %s", username));
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
