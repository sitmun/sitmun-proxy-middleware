package org.sitmun.proxy.middleware.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.List;

import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MapServiceUtils {
	
	@Autowired
	private List<RequestDecorator> decorators;


	public HttpResponse<InputStream> doRequest(OgcWmsPayloadDto ogcPayload) {
		HttpRequest request = createRequest(ogcPayload);
		HttpClient httpClient = HttpClient.newBuilder().build();
		try {
			return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private HttpRequest createRequest(OgcWmsPayloadDto ogcPayload) {
		Builder requestBuilder = HttpRequest.newBuilder();
		try {
			requestBuilder.uri(new URI(ogcPayload.getUri()));
			applyDecorators(requestBuilder, ogcPayload);
			if ("GET".equalsIgnoreCase(ogcPayload.getMethod())) {
				requestBuilder.GET();
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		return requestBuilder.build();
	}
	
	private void applyDecorators(Builder requestBuilder, OgcWmsPayloadDto ogcPayload) {
		decorators.forEach(d -> {
			if(d.accept(ogcPayload)) {
				d.apply(requestBuilder, ogcPayload);
			}
		});
	}
}
