package com.nonononoki.alovoa.containers;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.database.Database;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.vault.VaultContainer;
import io.restassured.response.Response;
import com.bettercloud.vault.VaultConfig;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class VaultContainerTest {
    private static final Logger logger = LoggerFactory.getLogger(VaultContainerTest.class);

    private static VaultContainer<?> vaultContainer;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    // Not so secret because for unit test only.
    private static final String VAULT_TOKEN = "xTjDMkMY78BABT6C6nb5uU0kt2rmwh6r";
    //private static final String VAULT_TOKEN = "hvs.CAESIO2w4_yOxSHmoGiTAJCK1IQv39-i-nD57LhmQPQO3MlHGh4KHGh2cy5yUWpSNko4V0ZMYTZscTRWNWZlaHMzbXg";
    private static final String OAUTH_PATH = "oauth-idp";
    private static final String OTHER_PATH = "alovoa/creds";


    @ClassRule
    public static VaultContainer<?> startVault() {

        vaultContainer = new VaultContainer<>("hashicorp/vault:1.14.1");
        vaultContainer.withVaultToken(VAULT_TOKEN);
        setSecrets();
        vaultContainer.withInitCommand("secrets enable transit", "write -f transit/keys/my-key");
        //vaultContainer.start();
        return vaultContainer;
    }

    private static void setSecrets() {
        vaultContainer
                .withSecretInVault(
                        "alovoa/creds",
                        "app.text.key=hwGEC7Bd1rseQtRTjgmMbeZYowAglhSD",
                        "app.text.salt=1JGKo5I7gKY6i9CR",
                        "app.admin.email=alovoa@example.com",
                        "app.login.remember.key=sKQP1DFetYQwOWAGrwYG0DKeLfvdXQYy",
                        "app.vapid.public=1234",
                        "app.vapid.private=12345678",
                        "spring.mail.password=3SeOx79Gwjlb26jCavGuv02OgaLZrfrY",
                        "spring.mail.username=alovoa@example.com",
                        "spring.data.redis.username=redis_user",
                        "spring.data.redis.password=redis_pass",
                        "app.admin.key=hYS1nHlcQgv9WxiY6dMiiuq7cF2619JW"
                );

    }

    SslConfig getSslConfig() throws IOException, VaultException {
        SslConfig sslConfig = new SslConfig();
        sslConfig.trustStoreFile(resourceLoader.getResource("classpath:truststore.jks").getFile());
        sslConfig.build();
        return sslConfig;
    }

    @Test
    public void test1() {
        logger.info("test1 begin");

        String host = vaultContainer.getHttpHostAddress();
        //String host = "https://vault.test.felsing.net:8200";
        String fullUrl = String.format("%s/v1/%s", host, "alovoa/creds/data");
        logger.info(String.format("Test1: %s", fullUrl));
        Response response = given()
            .header("X-Vault-Token", VAULT_TOKEN)
            .when()
            .get(fullUrl)
            .thenReturn();
        assertThat(response).isNotNull();

        logger.info(response.body().jsonPath().prettyPrint());

        Map<String, String> creds = response.body().jsonPath().getMap("data");
        creds.forEach((k, v) -> {
            logger.info(String.format("test1: %s : %s", k, v));
        });

        assertThat(creds.get("app.text.key")).isEqualTo("hwGEC7Bd1rseQtRTjgmMbeZYowAglhSD");

        logger.info("test1 end");
    }
}
