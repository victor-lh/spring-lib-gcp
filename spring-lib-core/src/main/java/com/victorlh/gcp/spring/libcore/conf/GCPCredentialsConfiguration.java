package com.victorlh.gcp.spring.libcore.conf;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.BindException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
		InputStream inputStream = getJsonIS();
		if (inputStream == null) {
			inputStream = getFileIS();
		}
		if (inputStream == null) {
			throw new IllegalStateException("[gcp.credentials.filePath] or [gcp.credentials.json] are required");
		}

		try {
			return GoogleCredentials.fromStream(inputStream);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				log.warn(e.getMessage(), e);
			}
		}

	}

	private InputStream getJsonIS() {
		String json = properties.getJson();
		if (json == null || json.trim().equals("")) {
			return null;
		}
		byte[] bytes = json.getBytes();
		return new ByteArrayInputStream(bytes);
	}

	private InputStream getFileIS() {
		String filePath = properties.getFilePath();
		if (filePath == null || filePath.trim().equals("")) {
			return null;
		}
		try {
			return new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
}
