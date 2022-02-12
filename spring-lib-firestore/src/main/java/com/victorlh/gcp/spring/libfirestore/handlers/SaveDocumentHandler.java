package com.victorlh.gcp.spring.libfirestore.handlers;

import com.google.cloud.firestore.DocumentSnapshot;
import com.victorlh.gcp.spring.libfirestore.utils.UtilFirestore;
import java.lang.reflect.Field;

public class SaveDocumentHandler extends AbstractDocumentHandler {
	@Override
	public void handle(Object model, DocumentSnapshot documentSnapshot) {
		Class<?> modelClass = model.getClass();
		if (!documentSnapshot.exists()) {
			Field createAtField = UtilFirestore.getCreateAtField(modelClass);
			if (createAtField != null) {
				setCurrentDate(createAtField, model);
			}
		}

		Field updateAtField = UtilFirestore.getUpdateAtField(modelClass);
		if (updateAtField != null) {
			setCurrentDate(updateAtField, model);
		}
	}

	public static void doHandle(Object model, DocumentSnapshot documentSnapshot) {
		new SaveDocumentHandler().handle(model, documentSnapshot);
	}
}
