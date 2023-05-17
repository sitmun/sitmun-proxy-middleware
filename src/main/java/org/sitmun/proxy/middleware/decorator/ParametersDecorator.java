package org.sitmun.proxy.middleware.decorator;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest.Builder;
import java.util.Map;

import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.springframework.stereotype.Component;

@Component
public class ParametersDecorator implements RequestDecorator {

	@Override
	public boolean accept(OgcWmsPayloadDto payload) {
		return payload.getParameters() != null && !payload.getParameters().isEmpty();
	}

	@Override
	public void apply(Builder requestBuilder, OgcWmsPayloadDto payload) {
		StringBuilder uri = new StringBuilder(payload.getUri().concat("?"));
		StringBuilder params = new StringBuilder();
		Map<String, String> parameters = payload.getParameters();
		parameters.keySet().forEach(k -> {
			params.append("&").append(k).append("=").append(parameters.get(k));
		});
		uri.append(params.substring(1));
		try {
			requestBuilder.uri(new URI(uri.toString()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

}
