package com.nonononoki.alovoa;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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
		SpringApplication.run(AlovoaApplication.class, args);
	}
}
