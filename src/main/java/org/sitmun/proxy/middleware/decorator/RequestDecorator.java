package org.sitmun.proxy.middleware.decorator;

import java.net.http.HttpRequest.Builder;

import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;

public interface RequestDecorator {
	
	public boolean accept(OgcWmsPayloadDto payload);
	
	public void apply(Builder requestBuilder, OgcWmsPayloadDto payload);
}
