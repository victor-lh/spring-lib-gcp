package com.victorlh.gcp.spring.libcore.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "gcp")
public class GCPCredentialsProperties {

	private String projectId;
}
