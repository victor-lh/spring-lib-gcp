package com.victorlh.gcp.spring.libcore.conf;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(GCPCredentialsProperties.class)
public class GCPCredentialsConfiguration {

	private final GCPCredentialsProperties properties;

	@Autowired
	public GCPCredentialsConfiguration(GCPCredentialsProperties properties) {
		this.properties = properties;
	}

	@Bean
	public GoogleCredentials getGoogleCredentials() {
		try {
			return GoogleCredentials.getApplicationDefault();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return null;
		}

	}
}
