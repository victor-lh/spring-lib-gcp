package com.victorlh.gcp.spring.libpubsub.publisher;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PubSubPublisher {

	private final GoogleCredentials googleCredentials;

	public ApiFuture<String> sendMessage(String topic, String message) {
		return sendMessage(topic, message, null);

	}

	public ApiFuture<String> sendMessage(String topic, String message, Map<String, String> attributes) {
		String projectId = getProjectId();
		TopicName topicName = TopicName.of(projectId, topic);

		Publisher publisher = null;
		try {
			publisher = Publisher.newBuilder(topicName)
					.setCredentialsProvider(() -> googleCredentials)
					.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ByteString data = ByteString.copyFromUtf8(message);
		PubsubMessage.Builder builder = PubsubMessage.newBuilder()
				.setData(data);

		if (attributes != null) {
			builder.putAllAttributes(attributes);
		}

		PubsubMessage pubsubMessage = builder.build();
		return publisher.publish(pubsubMessage);
	}


	private String getProjectId() {
		if (googleCredentials instanceof ServiceAccountCredentials) {
			return ((ServiceAccountCredentials) googleCredentials).getProjectId();
		}
		return "";
	}
}
