package com.victorlh.gcp.spring.libfirestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.victorlh.gcp.spring.libfirestore.errors.FirestoreError;
import com.victorlh.gcp.spring.libfirestore.handlers.SaveDocumentHandler;
import com.victorlh.gcp.spring.libfirestore.utils.UtilFirestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
public abstract class AbstractFirestoreRepository<T> {

	private final Class<T> parameterizedType;
	private final String collectionName;
	private final Firestore firestore;

	@Autowired
	@SuppressWarnings("unchecked")
	public AbstractFirestoreRepository(Firestore firestore) {
		ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
		this.parameterizedType = (Class<T>) type.getActualTypeArguments()[0];
		this.collectionName = UtilFirestore.getCollectionNameValue(this.parameterizedType);
		this.firestore = firestore;
	}

	/**
	 * Guarda el documento y devuelve el id
	 *
	 * @param model - modelo del documento a guardar
	 * @return Identificador del documento
	 */
	public String save(T model, String... collectionPathVariables) {
		CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		String documentId = UtilFirestore.getDocumentId(model);
		DocumentReference document = collectionReference.document(documentId);

		ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = document.get();
		DocumentSnapshot documentSnapshot = resolveFuture(documentSnapshotApiFuture);

		new SaveDocumentHandler().handle(model, documentSnapshot);

		ApiFuture<WriteResult> resultApiFuture = document.set(model);
		try {
			WriteResult writeResult = resultApiFuture.get();
			log.info("{} saved at {}", document.getPath(), writeResult.getUpdateTime());
			return documentId;
		} catch (InterruptedException | ExecutionException e) {
			String msg = String.format("Error saving %s %s", document.getPath(), e.getMessage());
			throw new FirestoreError(msg, e);
		}
	}

	public void delete(T model, String... collectionPathVariables) {
		CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		String documentId = UtilFirestore.getDocumentId(model);
		DocumentReference documentReference = collectionReference.document(documentId);
		ApiFuture<WriteResult> resultApiFuture = documentReference.delete();
		try {
			log.info("{} deleted at{}", documentReference.getPath(), resultApiFuture.get().getUpdateTime());
		} catch (InterruptedException | ExecutionException e) {
			String msg = String.format("Error saving %s=%s %s", collectionReference, documentId, e.getMessage());
			throw new FirestoreError(msg, e);
		}
	}

	public void recursiveDelete(String documentId, String... collectionPathVariables) {
		CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		DocumentReference documentReference = collectionReference.document(documentId);
		ApiFuture<Void> voidApiFuture = firestore.recursiveDelete(documentReference);
		try {
			log.info("{} recursive deleted at{}", documentReference.getPath(), voidApiFuture.get());
		} catch (InterruptedException | ExecutionException e) {
			String msg = String.format("Error saving %s=%s %s", collectionName, documentId, e.getMessage());
			throw new FirestoreError(msg, e);
		}
	}

	public List<T> findAll(String... collectionPathVariables) {
		CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		ApiFuture<QuerySnapshot> querySnapshotApiFuture = collectionReference.get();
		return extractQuery(querySnapshotApiFuture);
	}

	public List<T> findAll(CollectionPageRequest collectionPageRequest, String... collectionPathVariables) {
		CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		String orderByName = getOrderByName();
		return paginate(collectionReference, orderByName, collectionPageRequest, 20);
	}

	public Optional<T> findById(String documentId, String... collectionPathVariables) {
		CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		DocumentReference documentReference = collectionReference.document(documentId);
		ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = documentReference.get();
		DocumentSnapshot documentSnapshot = resolveFuture(documentSnapshotApiFuture);
		return Optional.ofNullable(toObject(documentSnapshot));
	}

	public Optional<T> findByReference(DocumentReference documentReference) {
		ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = documentReference.get();
		DocumentSnapshot documentSnapshot = resolveFuture(documentSnapshotApiFuture);
		return Optional.ofNullable(toObject(documentSnapshot));
	}

	public List<T> paginate(@NotNull Query query, @Nullable String orderBy, @NotNull CollectionPageRequest collectionPageRequest, int defaultLimit) {
		int limit = collectionPageRequest.getLimit() == null ? defaultLimit : collectionPageRequest.getLimit();
		int offset = collectionPageRequest.getOffset() == null ? 0 : collectionPageRequest.getOffset();

		if (orderBy != null) {
			query = query.orderBy(orderBy).limit(limit);
		} else {
			query = query.limit(limit);
		}
		ApiFuture<QuerySnapshot> querySnapshotApiFuture = query.offset(offset).get();
		return extractQuery(querySnapshotApiFuture);
	}

	public List<T> paginate(CollectionReference collectionReference, @Nullable String orderBy, CollectionPageRequest collectionPageRequest, int defaultLimit) {
		int limit = collectionPageRequest.getLimit() == null ? defaultLimit : collectionPageRequest.getLimit();
		int offset = collectionPageRequest.getOffset() == null ? 0 : collectionPageRequest.getOffset();

		Query query;
		if (orderBy != null) {
			query = collectionReference.orderBy(orderBy).limit(limit);
		} else {
			query = collectionReference.limit(limit);
		}
		ApiFuture<QuerySnapshot> querySnapshotApiFuture = query.offset(offset).get();
		return extractQuery(querySnapshotApiFuture);
	}

	public List<T> extractQuery(ApiFuture<QuerySnapshot> querySnapshotApiFuture) {
		QuerySnapshot queryDocumentSnapshots = resolveFuture(querySnapshotApiFuture);
		if (queryDocumentSnapshots == null) {
			return Collections.emptyList();
		}
		List<QueryDocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
		return documents.stream()
				.map(this::toObject)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public <Z> Z resolveFuture(ApiFuture<Z> future) {
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			String msg = String.format("Error getting %s, %s", collectionName, e.getMessage());
			throw new FirestoreError(msg, e);
		}
	}

	public CollectionReference getCollectionReference(String... collectionPathsValues) {
		String collectionName = getCollectionName();
		return UtilFirestore.parseCollectionReference(firestore, collectionName, collectionPathsValues);
	}

	public Firestore getFirestore() {
		return firestore;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public Class<T> getType() {
		return this.parameterizedType;
	}

	@Nullable
	public String getOrderByName() {
		return UtilFirestore.getOrderByField(getType());
	}

	@Nullable
	public T toObject(@Nullable DocumentSnapshot documentSnapshot) {
		if (documentSnapshot != null && documentSnapshot.exists()) {
			return documentSnapshot.toObject(getType());
		}
		return null;
	}
}