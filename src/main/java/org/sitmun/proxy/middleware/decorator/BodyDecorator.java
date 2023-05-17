package org.sitmun.proxy.middleware.decorator;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;

import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.springframework.stereotype.Component;

@Component
public class BodyDecorator implements RequestDecorator {

	@Override
	public boolean accept(OgcWmsPayloadDto payload) {
		//return "POST".equalsIgnoreCase(payload.getMethod()) && payload.getRequestBody() != null;
		return false;
	}

	@Override
	public void apply(Builder requestBuilder, OgcWmsPayloadDto payload) {
		// TODO Real implementation
		//Example
		//requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload.getRequestBody()));
	}

}
