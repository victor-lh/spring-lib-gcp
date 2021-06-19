package com.victorlh.gcp.spring.libfirestore.conf;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.victorlh.gcp.spring.libcore.conf.GCPCredentialsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GCPCredentialsConfiguration.class)
public class SpringFirestoreConf {

	private final GoogleCredentials googleCredentials;

	@Autowired
	public SpringFirestoreConf(GoogleCredentials googleCredentials) {
		this.googleCredentials = googleCredentials;
	}

	@Bean
	public Firestore getFirestore() {
		FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
				.setCredentials(googleCredentials)
				.build();
		return firestoreOptions.getService();
	}
}
