package com.nonononoki.alovoa;

import com.nonononoki.alovoa.config.VaultPropertySource;
import com.nonononoki.alovoa.model.AlovoaException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;


@SpringBootApplication
@EnableJpaRepositories("com.nonononoki.alovoa.repo")
@EntityScan("com.nonononoki.alovoa.entity")
@EnableCaching
@EnableScheduling
public class AlovoaApplication {

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.trustStore", "classpath:truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        Security.addProvider(new BouncyCastleProvider());

        new SpringApplicationBuilder()
                .sources(AlovoaApplication.class)
                .initializers(context -> {
                            try {
                                context
                                        .getEnvironment() // preserve default sources
                                        .getPropertySources() // preserve default sources
                                        .addFirst(VaultPropertySource.getInstance(context));
                            } catch (AlovoaException e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
                .run(args);
    }
}
