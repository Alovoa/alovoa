package com.nonononoki.alovoa.config;

import com.nonononoki.alovoa.model.AlovoaException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Resources
  https://cloud.spring.io/spring-cloud-vault/reference/html/
  https://www.baeldung.com/properties-with-spring
 */

public class VaultPropertySource extends PropertySource<String> {
    private static DatasourceCredentials datasourceCredentials;
    private static final Map<String, String> cachedVaultData = new HashMap<>();
    private static VaultPropertySource vaultPropertySource;
    private static String vaultPropertiesPath;

    private VaultPropertySource(String name, ConfigurableApplicationContext context)
            throws AlovoaException {
        super(name);
        datasourceCredentials = DatasourceCredentials.getInstance();
        ConfigurableEnvironment contextEnvironment = context.getEnvironment();
        String vaultToken = contextEnvironment.getProperty("spring.cloud.vault.token");
        vaultPropertiesPath = contextEnvironment.getProperty("app.vault.propertypath");
        if (vaultPropertiesPath ==null) { vaultPropertiesPath = "alovoa/creds"; }
        if (vaultToken == null) {
            throw new AlovoaException(
                    "spring.cloud.vault.token not defined in " +
                    "application.properties or environment variable" +
                    "'vault.token' not set"
            );
        }

        try {
            URI vaultUrl = new URI(Objects.requireNonNull(contextEnvironment.getProperty("spring.cloud.vault.uri")));
            datasourceCredentials.setVaultUrl(vaultUrl);
            datasourceCredentials.setVaultToken(vaultToken);
        } catch (URISyntaxException e) {
            throw new AlovoaException(String.format("'spring.cloud.vault.uri' contains invalid URI: %s", e.getMessage()));
        }
    }

    public static VaultPropertySource getInstance(ConfigurableApplicationContext context)
            throws AlovoaException {
        if (vaultPropertySource == null) {
            vaultPropertySource = new VaultPropertySource("vault", context);
        }
        return vaultPropertySource;
    }

    @Override
    public String getProperty(@Nullable String propertyName) {
        logger.trace(String.format("getProperty: %s", propertyName));
        if (cachedVaultData.containsKey(propertyName)) {
            logger.trace(String.format("Returned cached value for %s", propertyName));
            return cachedVaultData.get(propertyName);
        }

        // Check vault entries for required OAuth credentials
        Pattern pattern = Pattern.compile(
                "^spring\\.security\\.oauth2\\.client\\.registration\\.(\\w+)\\.(client-id|client-secret)$",
                Pattern.CASE_INSENSITIVE
        );
        assert propertyName != null;
        Matcher matcher = pattern.matcher(propertyName);
        if (matcher.matches()) {
            String idp = matcher.group(1);
            String type = matcher.group(2);
            String data = datasourceCredentials.getVaultCredentials(String.format("oauth-idp/%s", idp)).get(type);
            cachedVaultData.put(String.format("spring.security.oauth2.client.registration.%s.%s", idp, type), data);
            if (data==null) {
                logger.trace(String.format("getProperty: Cannot resolve %s from vault", propertyName));
            }
            return data;
        }

        String vaultValue = datasourceCredentials.getVaultCredentials(vaultPropertiesPath).get(propertyName);
        cachedVaultData.put(propertyName, vaultValue);
        if (vaultValue == null) {
            logger.trace(String.format("Vault does not contain a value for %s", propertyName));
        }
        return vaultValue;
    }

    public void clearCachedVaultProperties() {
        cachedVaultData.clear();
    }

}
