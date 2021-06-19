package com.victorlh.gcp.spring.libfirestore.errors;

public class FirestoreError extends RuntimeException {

	public FirestoreError(String msg) {
		super(msg);
	}

	public FirestoreError(String msg, Throwable cause) {
		super(msg, cause);
	}
}
