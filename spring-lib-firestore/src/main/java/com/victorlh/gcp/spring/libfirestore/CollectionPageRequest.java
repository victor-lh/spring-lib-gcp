package com.victorlh.gcp.spring.libfirestore;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class CollectionPageRequest {

	private final Integer limit;
	private final Integer offset;

}
