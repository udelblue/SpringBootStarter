package com.udelblue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.udelblue.backend.service.EmailService;
import com.udelblue.backend.service.SmtpEmailService;

@Configuration
@Profile("prod")
public class ProductionConfig {

	@Value("${stripe.prod.private.key}")
	private String stripeProdKey;

	@Bean
	public EmailService emailService() {
		return new SmtpEmailService();
	}

	@Bean
	public String stripeKey() {
		return stripeProdKey;
	}
}
