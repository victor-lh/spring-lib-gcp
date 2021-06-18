package com.victorlh.gcp.spring.libcore.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Configuration
@ConfigurationProperties(prefix = "gcp.credentials")
@Data
@Validated
public class GCPCredentialsProperties {

	@NotEmpty
	private String projectId;
	private String filePath;
	private String json;

}
