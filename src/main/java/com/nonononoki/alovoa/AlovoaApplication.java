package com.nonononoki.alovoa;

import com.nonononoki.alovoa.config.CustomPropertySource;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.URISyntaxException;
import java.security.Security;


@SpringBootApplication
@EnableJpaRepositories("com.nonononoki.alovoa.repo")
@EntityScan("com.nonononoki.alovoa.entity")
@EnableCaching
@EnableScheduling
public class AlovoaApplication {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		//SpringApplication.run(AlovoaApplication.class, args);

		new SpringApplicationBuilder()
				.sources(AlovoaApplication.class)
				.initializers(context -> {
							try {
								context
										.getEnvironment()
										.getPropertySources()
										.addLast(new CustomPropertySource("ip6li"));
							} catch (URISyntaxException e) {
								throw new RuntimeException(e);
							}
						}
				)
				.run(args);
	}

}
