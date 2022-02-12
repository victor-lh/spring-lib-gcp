package com.victorlh.gcp.spring.libfirestore.conf;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SpringFirestoreConf {

	@Bean
	public Firestore getFirestore() {
		FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance();
		return firestoreOptions.getService();
	}
}
