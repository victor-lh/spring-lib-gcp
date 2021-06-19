package com.victorlh.gcp.spring.libfirestore.utils;

import com.google.cloud.firestore.annotation.DocumentId;
import com.victorlh.gcp.spring.libfirestore.CollectionName;
import com.victorlh.gcp.spring.libfirestore.OrderBy;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
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
}
