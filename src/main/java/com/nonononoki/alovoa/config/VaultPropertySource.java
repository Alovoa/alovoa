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

    private static final List<String> VAULT_VARS = Arrays.asList(
            "app.text.key",
            "app.text.salt",
            "app.admin.email",
            "app.login.remember.key",
            "app.vapid.public",
            "app.vapid.private",
            "spring.mail.password",
            "spring.mail.username",
            "spring.data.redis.username",
            "spring.data.redis.password"
    );

    private VaultPropertySource(String name, ConfigurableApplicationContext context) throws AlovoaException {
        super(name);
        datasourceCredentials = DatasourceCredentials.getInstance();
        ConfigurableEnvironment contextEnvironment = context.getEnvironment();
        String vaultToken = contextEnvironment.getProperty("spring.cloud.vault.token");
        if (vaultToken == null) {
            throw new AlovoaException("spring.cloud.vault.token not defined in application.properties");
        }
        try {
            URI vaultUrl = new URI(Objects.requireNonNull(contextEnvironment.getProperty("spring.cloud.vault.uri")));
            datasourceCredentials.setVaultUrl(vaultUrl);
        } catch (URISyntaxException e) {
            logger.error(String.format("setVaultUrl %s failed", e.getMessage()));
        }
        datasourceCredentials.setVaultToken(vaultToken);
    }

    public static VaultPropertySource getInstance(ConfigurableApplicationContext context) throws AlovoaException {
        if (vaultPropertySource == null) {
            vaultPropertySource = new VaultPropertySource("vault", context);
        }
        return vaultPropertySource;
    }

    @Override
    public String getProperty(@Nullable String propertyName) {
        logger.trace(String.format("getProperty: %s", propertyName));
        String cachedData = cachedVaultData.get(propertyName);
        if (cachedData != null) return cachedData;

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
            String data = readOauthCredentials(idp, type);
            if (data!=null)
                cachedVaultData.put(String.format("spring.security.oauth2.client.registration.%s.%s", idp, type), data);
            else
                logger.warn(String.format("getProperty: Cannot resolve %s from vault", propertyName));
            return data;
        }

        if (VAULT_VARS.contains(propertyName.toLowerCase())) {
            String secret = readOtherCredentials("alovoa/creds", propertyName);
            cachedVaultData.put(propertyName, secret);
            return secret;
        }

        // No customization: Return null
        return null;
    }

    private String readOauthCredentials(String idp, String type) {
        return datasourceCredentials.getOAuthCredentials(String.format("oauth-idp/%s", idp)).get(type);
    }

    private String readOtherCredentials(String path, String type) {
        return datasourceCredentials.getOAuthCredentials(path).get(type);
    }

}
