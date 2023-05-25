package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.request.GlobalRequest;
import org.springframework.stereotype.Component;

@Component
public class BodyDecorator implements RequestDecorator {

	@Override
	public boolean accept(PayloadDto payload) {
		boolean result = false;
		if(payload instanceof OgcWmsPayloadDto) {
			OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;
			//result = "POST".equalsIgnoreCase(ogcPayload.getMethod()) && ogcPayload.getRequestBody() != null;
		}
		return result;
	}

	@Override
	public void apply(GlobalRequest globalRequest, PayloadDto payload) {
		// TODO Valid implementation
		//Example
/*		OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;		
		globalRequest.getCustomHttpRequest().getRequestBuilder()
			.post(ogcPayload.getRequestBody());*/
	}

}
