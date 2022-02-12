package com.victorlh.gcp.spring.libfirestore.handlers;

import java.lang.reflect.Field;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class AbstractDocumentHandler implements FirestoreHandler {

	protected void setCurrentDate(Field field, Object model) {
		field.setAccessible(true);
		if (!field.getType().isAssignableFrom(Date.class)) {
			throw new IllegalArgumentException("El campo fecha tiene que ser de tipo java.util.Date");
		}
		Date date = new Date(System.currentTimeMillis());
		try {
			field.set(model, date);
		} catch (IllegalAccessException e) {
			log.warn(e.getMessage(), e);
		}
	}
}
