package com.victorlh.gcp.spring.libfirestore.handlers;

import com.google.cloud.firestore.DocumentSnapshot;

public interface FirestoreHandler {

	void handle(Object model, DocumentSnapshot documentSnapshot);

}
