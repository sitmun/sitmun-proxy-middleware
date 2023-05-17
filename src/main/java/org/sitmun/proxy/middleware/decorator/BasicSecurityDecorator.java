package org.sitmun.proxy.middleware.decorator;

import java.net.http.HttpRequest.Builder;
import java.util.Base64;

import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BasicSecurityDecorator implements RequestDecorator {

	@Override
	public boolean accept(OgcWmsPayloadDto payload) {
		return payload.getSecurity() != null
				&& StringUtils.hasText(payload.getSecurity().getUsername())
				&& StringUtils.hasText(payload.getSecurity().getPassword());
	}

	@Override
	public void apply(Builder requestBuilder, OgcWmsPayloadDto payload) {
		String authString = payload.getSecurity().getUsername().concat(":").concat(payload.getSecurity().getPassword());
		String authEncode = encodeAuthorization(authString);
		requestBuilder.header("Authorization", "Basic ".concat(authEncode));
	}
	
	private String encodeAuthorization(String authorization) {
		return Base64.getEncoder().encodeToString(authorization.getBytes());
	}
}
