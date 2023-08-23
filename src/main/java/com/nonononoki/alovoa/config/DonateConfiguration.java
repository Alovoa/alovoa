package com.nonononoki.alovoa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "donate")
public class DonateConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(DonateConfiguration.class);

    private static boolean donateEnabled;

    @Value("${donate.enabled:false}")
    private void donateEnabled(String s) { donateEnabled = Boolean.parseBoolean(s); }

    @Bean(name = "donateEnabled")
    public DonateConfigurationEnabledIntf beanDonateEnabled() {

        return () -> Boolean.toString(donateEnabled);
    }

}
