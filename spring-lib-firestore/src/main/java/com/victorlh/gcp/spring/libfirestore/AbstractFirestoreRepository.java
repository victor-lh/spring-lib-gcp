package com.victorlh.gcp.spring.libfirestore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.victorlh.gcp.spring.libfirestore.errors.FirestoreError;
import com.victorlh.gcp.spring.libfirestore.handlers.SaveDocumentHandler;
import com.victorlh.gcp.spring.libfirestore.utils.UtilFirestore;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public abstract class AbstractFirestoreRepository<T> {

	private final Class<T> parameterizedType;
	private final String collectionName;
	private final Firestore firestore;

	@Autowired
	@SuppressWarnings("unchecked")
	protected AbstractFirestoreRepository(Firestore firestore) {
		final ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
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
	public Mono<String> save(T model, String... collectionPathVariables) {
		final CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		final String documentId = UtilFirestore.getDocumentId(model);
		final DocumentReference document = collectionReference.document(documentId);

		return futureToMono(document::get)
				.doOnNext(documentSnapshot -> log.trace("Save operation Get document. [Id: {}] [Exists: {}]", documentId, documentSnapshot.exists()))
				.doOnNext(documentSnapshot -> SaveDocumentHandler.doHandle(model, documentSnapshot))
				.flatMap(documentSnapshot -> futureToMono(() -> document.set(model)))
				.doOnNext(writeResult -> log.info("{} saved at {}", document.getPath(), writeResult.getUpdateTime()))
				.map(writeResult -> documentId);
	}

	public Mono<Void> delete(T model, String... collectionPathVariables) {
		final String documentId = UtilFirestore.getDocumentId(model);
		return delete(documentId, collectionPathVariables);
	}

	public Mono<Void> delete(String documentId, String... collectionPathVariables) {
		final CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		final DocumentReference documentReference = collectionReference.document(documentId);
		return futureToMono(documentReference::delete)
				.doOnNext(writeResult -> log.info("{} deleted at {}", documentReference.getPath(), writeResult.getUpdateTime()))
				.flatMap(writeResult -> Mono.empty());
	}

	public Mono<Void> recursiveDelete(String documentId, String... collectionPathVariables) {
		final CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		final DocumentReference documentReference = collectionReference.document(documentId);
		return futureToMono(() -> firestore.recursiveDelete(documentReference))
				.doOnNext(unused -> log.info("{} recursive deleted", documentReference.getPath()))
				.flatMap(unused -> Mono.empty());
	}

	public Flux<T> findAll(String... collectionPathVariables) {
		final CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		return extractQuery(collectionReference::get);
	}

	public Flux<T> findAll(CollectionPageRequest collectionPageRequest, String... collectionPathVariables) {
		final CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		final String orderByName = getOrderByName();
		return paginate(collectionReference, orderByName, collectionPageRequest, 20);
	}

	public Mono<T> findById(String documentId, String... collectionPathVariables) {
		final CollectionReference collectionReference = getCollectionReference(collectionPathVariables);
		final DocumentReference documentReference = collectionReference.document(documentId);
		return futureToMono(documentReference::get)
				.mapNotNull(this::toObject);
	}

	public Mono<T> findByReference(DocumentReference documentReference) {
		return futureToMono(documentReference::get)
				.mapNotNull(this::toObject);
	}

	public Flux<T> paginate(final Query query, @Nullable String orderBy, CollectionPageRequest collectionPageRequest, int defaultLimit) {
		return extractQuery(() -> {
			final int limit = collectionPageRequest.getLimit() == null ? defaultLimit : collectionPageRequest.getLimit();
			final int offset = collectionPageRequest.getOffset() == null ? 0 : collectionPageRequest.getOffset();

			Query internalQuery = query;
			if (orderBy != null) {
				internalQuery = internalQuery.orderBy(orderBy).limit(limit);
			} else {
				internalQuery = internalQuery.limit(limit);
			}
			return internalQuery.offset(offset).get();
		});
	}

	public Flux<T> paginate(CollectionReference collectionReference, @Nullable String orderBy, CollectionPageRequest collectionPageRequest, int defaultLimit) {
		return extractQuery(() -> {
			final int limit = collectionPageRequest.getLimit() == null ? defaultLimit : collectionPageRequest.getLimit();
			final int offset = collectionPageRequest.getOffset() == null ? 0 : collectionPageRequest.getOffset();

			final Query query;
			if (orderBy != null) {
				query = collectionReference.orderBy(orderBy).limit(limit);
			} else {
				query = collectionReference.limit(limit);
			}
			return query.offset(offset).get();
		});
	}

	public Flux<T> extractQuery(Supplier<ApiFuture<QuerySnapshot>> supplier) {
		return futureToMono(supplier)
				.flatMapIterable(QuerySnapshot::getDocuments)
				.mapNotNull(this::toObject);
	}

	public <Z> Mono<Z> futureToMono(Supplier<ApiFuture<Z>> supplier) {
		final CompletableFuture<Z> completableFuture = CompletableFuture.supplyAsync(() -> {
			try {
				final ApiFuture<Z> zApiFuture = supplier.get();
				return zApiFuture.get();
			} catch (ExecutionException e) {
				final String msg = String.format("Firestore error %s, %s", collectionName, e.getMessage());
				throw new FirestoreError(msg, e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				final String msg = String.format("Firestore error %s, %s", collectionName, e.getMessage());
				throw new FirestoreError(msg, e);
			}
		});
		return Mono.fromFuture(completableFuture);
	}

	public CollectionReference getCollectionReference(String... collectionPathsValues) {
		return UtilFirestore.parseCollectionReference(this.firestore, this.collectionName, collectionPathsValues);
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
	public T toObject(@Nonnull DocumentSnapshot documentSnapshot) {
		if (documentSnapshot.exists()) {
			return documentSnapshot.toObject(getType());
		}
		return null;
	}

}
