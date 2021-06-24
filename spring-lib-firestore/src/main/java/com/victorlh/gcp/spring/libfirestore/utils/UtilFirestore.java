package com.victorlh.gcp.spring.libfirestore.utils;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.annotation.DocumentId;
import com.victorlh.gcp.spring.libfirestore.anotations.CollectionName;
import com.victorlh.gcp.spring.libfirestore.anotations.CreateAt;
import com.victorlh.gcp.spring.libfirestore.anotations.OrderBy;
import com.victorlh.gcp.spring.libfirestore.anotations.UpdateAt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

@Slf4j
public class UtilFirestore {
	private UtilFirestore() {
	}

	public static String getCollectionNameValue(Class<?> tClass) {
		if (!tClass.isAnnotationPresent(CollectionName.class)) {
			throw new IllegalArgumentException("La etiqueta CollectionName no existe");
		}

		return tClass.getAnnotation(CollectionName.class).value();
	}

	@Nullable
	public static String getOrderByField(Class<?> tClass) {
		return Arrays.stream(tClass.getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(OrderBy.class))
				.findFirst()
				.map(Field::getName)
				.orElse(null);
	}

	@Nullable
	public static Field getCreateAtField(Class<?> clazz) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(CreateAt.class))
				.findFirst()
				.orElse(null);
	}

	@Nullable
	public static Field getUpdateAtField(Class<?> clazz) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(UpdateAt.class))
				.findFirst()
				.orElse(null);
	}

	@Nullable
	public static Field getDocumentIdField(Class<?> clazz) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(DocumentId.class))
				.findFirst()
				.orElse(null);
	}

	private static Object getKeyFromFields(Class<?> clazz, Object t) {
		Field documentIdField = getDocumentIdField(clazz);
		if (documentIdField == null) {
			return null;
		}
		documentIdField.setAccessible(true);
		try {
			return documentIdField.get(t);
		} catch (IllegalAccessException e) {
			log.error("Error in getting documentId key", e);
		}
		return null;
	}

	public static String getDocumentId(Object t) {
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

	public static CollectionReference parseCollectionReference(Firestore firestore, String collectionPath, String... collectionPathValue) {
		String[] pathSegments = StringUtils.split(collectionPath, "/");

		List<String> listPathValue = Arrays.asList(collectionPathValue);
		Queue<String> pathValueQueue = new LinkedList<>(listPathValue);

		CollectionReference collectionReference = null;
		DocumentReference document = null;
		for (String pathSegment : pathSegments) {
			if (StringUtils.startsWith(pathSegment, "{") && StringUtils.endsWith(pathSegment, "}")) {
				if (collectionReference == null) {
					throw new IllegalArgumentException("Formato de collectionPath invalido");
				}
				String poll = pathValueQueue.poll();
				if (StringUtils.isBlank(poll)) {
					throw new IllegalArgumentException("El numero de argumentos no es valido");
				}

				document = collectionReference.document(poll);
				collectionReference = null;
			} else {
				if (document == null) {
					collectionReference = firestore.collection(pathSegment);
				} else {
					collectionReference = document.collection(pathSegment);
				}
				document = null;
			}
		}

		if (collectionReference == null) {
			throw new IllegalArgumentException("Formato de collectionPath invalido");
		}

		return collectionReference;
	}
}
