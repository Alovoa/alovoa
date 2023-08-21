package com.nonononoki.alovoa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "chat")
public class ChatConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    public enum CHAT_TYPE {INTERNAL, MATRIX}

    private static boolean chatEnabled;
    private static CHAT_TYPE chatType;

    @Value("${chat.enabled:false}")
    private void chatEnabled(String s) { chatEnabled = Boolean.parseBoolean(s); }

    @Value("${chat.type:INTERNAL}")
    private void chatType(String s) { chatType = CHAT_TYPE.valueOf(s.toUpperCase()); }

    @Bean(name = "chatEnabled")
    public ChatConfigurationEnabledIntf beanChatEnabled() {
        return () -> Boolean.toString(chatEnabled);
    }

    @Bean(name = "chatUrl")
    public ChatConfigurationUrlIntf beanChatUrl() {
        switch (chatType) {
            case INTERNAL -> {
                return () -> "/chats";
            }
            case MATRIX -> {
                return () -> "/element-web/index.html";
            }
            default -> {
                return () -> "#";
            }
        }
    }

}
