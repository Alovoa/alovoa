package com.nonononoki.alovoa.config;

import org.springframework.core.env.PropertySource;

import java.net.URI;
import java.net.URISyntaxException;

public class CustomPropertySource extends PropertySource<String> {

    private static MysqlCredentials mysqlCredentials;

    public CustomPropertySource(String name) throws URISyntaxException {
        super(name);
        mysqlCredentials = MysqlCredentials.getInstance();
        mysqlCredentials.setVaultUrl(new URI("https://vault.test.felsing.net:8200"));
        mysqlCredentials.setVaultToken("hvs.CAESIO2w4_yOxSHmoGiTAJCK1IQv39-i-nD57LhmQPQO3MlHGh4KHGh2cy5yUWpSNko4V0ZMYTZscTRWNWZlaHMzbXg");
    }

    @Override
    public Object getProperty(String name) {
        return mysqlCredentials.getOAuthCredentials(String.format("oauth-idp/%s", name));
    }
}
