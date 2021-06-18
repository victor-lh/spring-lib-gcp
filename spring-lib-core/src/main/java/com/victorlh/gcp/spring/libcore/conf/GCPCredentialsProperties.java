package com.victorlh.gcp.spring.libcore.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gcp.credentials")
@Data
public class GCPCredentialsProperties {

	private String projectId;
	private String filePath;
	private String json;

}
