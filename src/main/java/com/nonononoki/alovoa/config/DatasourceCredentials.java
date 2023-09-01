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
import java.util.HashMap;
import java.util.Map;

@Component
public class DatasourceCredentials {
    private static final Logger logger = LoggerFactory.getLogger(DatasourceCredentials.class);

    private static DatasourceCredentials datasourceCredentials;
    private static String username;
    private static String password;

    private static URI vaultUrl;
    private static String vaultToken;
    private static String vaultTemplatePath;

    private DatasourceCredentials() {}

    public static DatasourceCredentials getInstance() {

        if (datasourceCredentials == null) {
            logger.info(String.format("Created new %s instance", DatasourceCredentials.class));
            datasourceCredentials = new DatasourceCredentials();
        } else {
            logger.info(String.format("Using existing %s instance", DatasourceCredentials.class));
        }
        return datasourceCredentials;
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

    private VaultTemplate getVaultTemplate() {
        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setHost(vaultUrl.getHost());
        vaultEndpoint.setScheme(vaultUrl.getScheme());
        vaultEndpoint.setPort(vaultUrl.getPort());
        return new VaultTemplate(
                vaultEndpoint,
                new TokenAuthentication(vaultToken)
        );
    }

    @Scheduled(fixedDelay = 300000)
    public void update() {
        VaultTemplate vaultTemplate = getVaultTemplate();
        VaultResponse vaultResponse = vaultTemplate.read(vaultTemplatePath);
        assert vaultResponse != null;
        Map<String, Object> vaultResponseData = vaultResponse.getData();
        assert vaultResponseData != null;
        username = vaultResponseData.get("username").toString();
        password = vaultResponseData.get("password").toString();
        logger.debug(String.format("Database credentials updated with username %s", username));
    }

    public Map<String, String> getVaultCredentials(String vaultPath) {
        VaultTemplate vaultTemplate = getVaultTemplate();
        VaultResponse vaultResponse = vaultTemplate.read(vaultPath);
        assert vaultResponse != null;
        Map<String, Object> vaultResponseData = vaultResponse.getData();
        Map<String, String> dataMap = new HashMap<>();
        assert vaultResponseData != null;
        vaultResponseData.forEach((k, v) -> {
            dataMap.put(k, v.toString());
        });
        return dataMap;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
