package com.victorlh.gcp.spring.libfirestore.handlers;

import com.google.cloud.firestore.DocumentSnapshot;

import javax.validation.constraints.NotNull;

public interface FirestoreHandler {

	void handle(@NotNull Object model,@NotNull DocumentSnapshot documentSnapshot);

}
