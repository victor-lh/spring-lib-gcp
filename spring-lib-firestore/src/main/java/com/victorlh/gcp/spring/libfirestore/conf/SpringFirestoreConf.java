package com.victorlh.gcp.spring.libfirestore.conf;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.victorlh.gcp.spring.libcore.conf.GCPCredentialsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SpringFirestoreConf {

	private final GCPCredentialsProperties properties;

	@Bean
	public Firestore getFirestore() {
		return FirestoreOptions.newBuilder()
				.setProjectId(properties.getProjectId())
				.build()
				.getService();
	}
}
