package org.sitmun.proxy.middleware.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("OgcWmsPayload")
public class OgcWmsPayloadDto extends PayloadDto {

	private String uri;
	
	private String method;
	
	private Map<String, String> parameters;
	
	private HttpSecurityDto security;
}
