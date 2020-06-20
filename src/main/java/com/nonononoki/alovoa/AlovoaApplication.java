package com.nonononoki.alovoa;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("com.nonononoki.alovoa.repo")
@EntityScan("com.nonononoki.alovoa.entity")
@EnableCaching
public class AlovoaApplication {

	public static void main(String[] args) {
		
		Security.addProvider(new BouncyCastleProvider());
		SpringApplication.run(AlovoaApplication.class, args);
	}
}
