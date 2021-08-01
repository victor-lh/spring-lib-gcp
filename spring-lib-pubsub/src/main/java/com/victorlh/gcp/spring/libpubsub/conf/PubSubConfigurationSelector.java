package com.victorlh.gcp.spring.libpubsub.conf;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;

@Order
public class PubSubConfigurationSelector implements DeferredImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return new String[]{PubSubBootstrapConfiguration.class.getName()};
	}

}
