package com.victorlh.gcp.spring.libfirestore.conf;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.victorlh.gcp.spring.libcore.conf.GCPCredentialsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringFirestoreConf {

	private final GCPCredentialsProperties gcpCredentialsProperties;
	private final GoogleCredentials googleCredentials;

	@Autowired
	public SpringFirestoreConf(GCPCredentialsProperties gcpCredentialsProperties, GoogleCredentials googleCredentials) {
		this.gcpCredentialsProperties = gcpCredentialsProperties;
		this.googleCredentials = googleCredentials;
	}

	@Bean
	public Firestore getFirestore() {
		FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
				.setProjectId(gcpCredentialsProperties.getProjectId())
				.setCredentials(googleCredentials)
				.build();
		return firestoreOptions.getService();
	}
}
