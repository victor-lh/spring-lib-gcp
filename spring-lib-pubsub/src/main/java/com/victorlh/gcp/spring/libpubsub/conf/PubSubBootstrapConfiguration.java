package com.victorlh.gcp.spring.libpubsub.conf;

import com.victorlh.gcp.spring.libpubsub.consumer.PubSubListenerAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class PubSubBootstrapConfiguration implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(PubSubListenerAnnotationBeanPostProcessor.PUBSUB_LISTENER_ANNOTATION_BEAN_NAME)) {
			registry.registerBeanDefinition(PubSubListenerAnnotationBeanPostProcessor.PUBSUB_LISTENER_ANNOTATION_BEAN_NAME, new RootBeanDefinition(PubSubListenerAnnotationBeanPostProcessor.class));
		}
	}

}
