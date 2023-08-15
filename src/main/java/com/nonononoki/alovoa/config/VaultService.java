package com.nonononoki.alovoa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import java.net.URI;
import java.util.Map;

public class VaultService {
    private static final Logger logger = LoggerFactory.getLogger(VaultService.class);

    private final VaultTemplate vaultTemplate;

    public VaultService(URI vaultUri, String vaultToken) {
        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setHost(vaultUri.getHost());
        vaultEndpoint.setScheme(vaultUri.getScheme());
        vaultEndpoint.setPort(vaultUri.getPort());
        vaultTemplate = new VaultTemplate(
                vaultEndpoint,
                new TokenAuthentication(vaultToken)
        );
        logger.debug(String.format("Created VaultService with URL %s", vaultUri.toString()));
    }

    public MysqlCredentials getMysqlCredentials() {
        MysqlCredentials mysqlCredentials = new MysqlCredentials();
        VaultResponse vaultResponse = vaultTemplate.read("database/creds/mysqlrole");
        assert vaultResponse != null;
        Map<String, Object> vaultResponseData = vaultResponse.getData();
        assert vaultResponseData != null;
        mysqlCredentials.username = vaultResponseData.get("username").toString();
        mysqlCredentials.password = vaultResponseData.get("password").toString();
        return mysqlCredentials;
    }

}
