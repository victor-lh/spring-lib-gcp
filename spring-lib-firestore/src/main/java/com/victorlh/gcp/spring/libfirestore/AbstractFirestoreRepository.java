package com.victorlh.gcp.spring.libfirestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
public abstract class AbstractFirestoreRepository<T> {

	private final CollectionReference collectionReference;
	private final String collectionName;
	private final Class<T> parameterizedType;

	@Autowired
	protected AbstractFirestoreRepository(Firestore firestore) {
		this.parameterizedType = getParameterizedType();
		this.collectionName = getCollectionNameValue(this.parameterizedType);
		this.collectionReference = firestore.collection(this.collectionName);
	}

	public boolean save(T model) {
		String documentId = getDocumentId(model);
		ApiFuture<WriteResult> resultApiFuture = collectionReference.document(documentId).set(model);
		try {
			log.info("{}-{} saved at{}", collectionName, documentId, resultApiFuture.get().getUpdateTime());
			return true;
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error saving {}={} {}", collectionName, documentId, e.getMessage());
		}
		return false;
	}

	public boolean delete(T model) {
		String documentId = getDocumentId(model);
		ApiFuture<WriteResult> resultApiFuture = collectionReference.document(documentId).delete();
		try {
			log.info("{}-{} saved at{}", collectionName, documentId, resultApiFuture.get().getUpdateTime());
			return true;
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error saving {}={} {}", collectionName, documentId, e.getMessage());
		}
		return false;
	}

	public List<T> findAll() {
		ApiFuture<QuerySnapshot> querySnapshotApiFuture = collectionReference.get();

		try {
			List<QueryDocumentSnapshot> queryDocumentSnapshots = querySnapshotApiFuture.get().getDocuments();

			return queryDocumentSnapshots.stream()
					.map(queryDocumentSnapshot -> queryDocumentSnapshot.toObject(parameterizedType))
					.collect(Collectors.toList());

		} catch (InterruptedException | ExecutionException e) {
			log.error("Exception occurred while retrieving all document for {}", collectionName);
		}
		return Collections.<T>emptyList();

	}


	public Optional<T> findById(String documentId) {
		DocumentReference documentReference = collectionReference.document(documentId);
		ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = documentReference.get();

		try {
			DocumentSnapshot documentSnapshot = documentSnapshotApiFuture.get();

			if (documentSnapshot.exists()) {
				return Optional.ofNullable(documentSnapshot.toObject(parameterizedType));
			}

		} catch (InterruptedException | ExecutionException e) {
			log.error("Exception occurred retrieving: {} {}, {}", collectionName, documentId, e.getMessage());
		}

		return Optional.empty();

	}

	protected static String getDocumentId(Object t) {
		Object key;
		Class<?> clzz = t.getClass();
		do {
			key = getKeyFromFields(clzz, t);
			clzz = clzz.getSuperclass();
		} while (key == null && clzz != null);

		if (key == null) {
			return UUID.randomUUID().toString();
		}
		return String.valueOf(key);
	}

	private static Object getKeyFromFields(Class<?> clazz, Object t) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(DocumentId.class))
				.findFirst()
				.map(field -> getValue(t, field))
				.orElse(null);
	}

	@Nullable
	private static Object getValue(Object t, java.lang.reflect.Field field) {
		field.setAccessible(true);
		try {
			return field.get(t);
		} catch (IllegalAccessException e) {
			log.error("Error in getting documentId key", e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Class<T> getParameterizedType() {
		ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
		return (Class<T>) type.getActualTypeArguments()[0];
	}

	protected CollectionReference getCollectionReference() {
		return this.collectionReference;
	}

	protected Class<T> getType() {
		return this.parameterizedType;
	}

	private static String getCollectionNameValue(Class<?> tClass) {
		if (!tClass.isAnnotationPresent(CollectionName.class)) {
			throw new IllegalArgumentException("La etiqueta CollectionName no existe");
		}

		return tClass.getAnnotation(CollectionName.class).value();
	}
}