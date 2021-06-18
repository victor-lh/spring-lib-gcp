package com.victorlh.gcp.spring.libfirestore;

import com.victorlh.gcp.spring.libfirestore.conf.SpringFirestoreConf;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SpringFirestoreConf.class)
public @interface EnableFirestore {
}