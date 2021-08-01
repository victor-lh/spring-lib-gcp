package com.victorlh.gcp.spring.libpubsub.consumer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class PubSubListenerAnnotationBeanPostProcessor implements BeanPostProcessor {

	public static final String PUBSUB_LISTENER_ANNOTATION_BEAN_NAME = "PubSubListenerAnnotationBeanPostProcessor";

	private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

	private final GoogleCredentials googleCredentials;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> beanClass = bean.getClass();
		if (!this.nonAnnotatedClasses.contains(beanClass)) {
			Class<?> targetClass = AopUtils.getTargetClass(bean);
			List<MethodListener> methods = findMethods(targetClass);

			if (methods.isEmpty()) {
				this.nonAnnotatedClasses.add(bean.getClass());
				log.trace("No @PubSubListener annotations found on bean type: " + bean.getClass());
			} else {
				methods.forEach(m -> processMethod(m, bean));
			}
		}
		return bean;
	}

	private void processMethod(MethodListener method, Object bean) {
		String[] subscriptions = method.getSubscriptions();
		for (String sub : subscriptions) {
			newSubscriber(sub, method.getMethod(), bean);
		}
	}

	private void newSubscriber(String subscription, Method method, Object bean) {
		ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(getProjectId(), subscription);
		Subscriber subscriber = Subscriber
				.newBuilder(subscriptionName, (pubsubMessage, ackReplyConsumer) -> receiveMessage(pubsubMessage, ackReplyConsumer, method, bean))
				.setCredentialsProvider(() -> googleCredentials)
				.build();
		subscriber.startAsync();
	}

	public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer, Method method, Object bean) {
		log.trace("PUB/SUB Message Receive: {}", pubsubMessage.getMessageId());

		Class<?>[] parameterTypes = method.getParameterTypes();
		Object[] values = new Object[parameterTypes.length];

		for (int i = 0; i < parameterTypes.length; i++) {
			if (parameterTypes[i].isAssignableFrom(PubsubMessage.class)) {
				values[i] = pubsubMessage;
			} else {
				ByteString data = pubsubMessage.getData();
				values[i] = data.toString(Charset.defaultCharset());
			}
		}

		try {
			method.invoke(bean, values);
			ackReplyConsumer.ack();
		} catch (IllegalAccessException | InvocationTargetException e) {
			log.error("PUB/SUB Message [{}] invoke error", pubsubMessage.getMessageId(), e);
		}
	}

	private String getProjectId() {
		if (googleCredentials instanceof ServiceAccountCredentials) {
			return ((ServiceAccountCredentials) googleCredentials).getProjectId();
		}
		return "";
	}

	private static List<MethodListener> findMethods(final Class<?> type) {
		final List<MethodListener> methods = new ArrayList<>();
		Class<?> klass = type;
		while (klass != Object.class) {
			for (final Method method : klass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(PubSubListener.class)) {
					MethodListener methodListener = validateMethod(method);
					methods.add(methodListener);
				}
			}
			klass = klass.getSuperclass();
		}
		return methods;
	}

	private static MethodListener validateMethod(Method method) {
		Class<?> clazz = method.getDeclaringClass();
		PubSubListener annotation = method.getAnnotation(PubSubListener.class);
		String[] subscriptions = annotation.subscriptions();

		if (subscriptions.length == 0) {
			String error = String.format("Method %s annotate with @PubSubListener in class %s has not subscriptions", method.getName(), clazz.getSimpleName());
			throw new IllegalArgumentException(error);
		}

		boolean hasPub = false;
		boolean hasString = false;

		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			if (i > 1) {
				String error = String.format("Method %s annotate with @PubSubListener in class %s has more that 2 parameters", method.getName(), clazz.getSimpleName());
				throw new IllegalArgumentException(error);
			}

			if (!parameterTypes[i].isAssignableFrom(PubsubMessage.class) && !parameterTypes[i].isAssignableFrom(String.class)) {
				String error = String.format("Method %s annotate with @PubSubListener in class %s parameters not allowed, only [%s, %s]", method.getName(), clazz.getSimpleName(), PubsubMessage.class.getSimpleName(), String.class.getSimpleName());
				throw new IllegalArgumentException(error);
			} else {
				if (parameterTypes[i].isAssignableFrom(PubsubMessage.class)) {
					if (hasPub) {
						String error = String.format("Method %s annotate with @PubSubListener in class %s parameters not allowed, only [%s, %s]", method.getName(), clazz.getSimpleName(), PubsubMessage.class.getSimpleName(), String.class.getSimpleName());
						throw new IllegalArgumentException(error);
					} else {
						hasPub = true;
					}
				} else {
					if (hasString) {
						String error = String.format("Method %s annotate with @PubSubListener in class %s parameters not allowed, only [%s, %s]", method.getName(), clazz.getSimpleName(), PubsubMessage.class.getSimpleName(), String.class.getSimpleName());
						throw new IllegalArgumentException(error);
					} else {
						hasString = true;
					}
				}
			}
		}

		return new MethodListener(method, subscriptions);

	}

	@RequiredArgsConstructor
	@Getter
	private static class MethodListener {
		private final Method method;
		private final String[] subscriptions;
	}
}
