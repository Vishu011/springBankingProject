package com.transaction.config;

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Provides a JavaMailSender bean if Spring Boot's auto-config doesn't create one.
 * - If spring.mail.* properties are present, they are applied.
 * - If not present, a default JavaMailSenderImpl is returned so the app can start.
 *   Actual email sending may fail until mail properties are configured.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    private final MailProperties mailProperties;

    public MailConfig(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        if (mailProperties.getHost() != null && !mailProperties.getHost().isBlank()) {
            sender.setHost(mailProperties.getHost());
            sender.setPort(mailProperties.getPort());
            sender.setUsername(mailProperties.getUsername());
            sender.setPassword(mailProperties.getPassword());
            Properties props = new Properties();
            if (mailProperties.getProperties() != null) {
                props.putAll(mailProperties.getProperties());
            }
            sender.setJavaMailProperties(props);
        }
        return sender;
    }
}
