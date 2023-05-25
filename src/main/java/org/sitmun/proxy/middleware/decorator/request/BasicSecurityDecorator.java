package org.sitmun.proxy.middleware.decorator.request;

import java.util.Base64;

import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.request.GlobalRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BasicSecurityDecorator implements RequestDecorator {

	@Override
	public boolean accept(PayloadDto payload) {
		boolean result = false;
		if (payload instanceof OgcWmsPayloadDto) {
			OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;
			result = ogcPayload.getSecurity() != null
					&& StringUtils.hasText(ogcPayload.getSecurity().getUsername())
					&& StringUtils.hasText(ogcPayload.getSecurity().getPassword());
		}
		return result;
	}

	@Override
	public void apply(GlobalRequest globalRequest, PayloadDto payload) {
		OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;
		String authString = ogcPayload.getSecurity().getUsername().concat(":").concat(ogcPayload.getSecurity().getPassword());
		String authEncode = encodeAuthorization(authString);
		globalRequest.getCustomHttpRequest().getRequestBuilder()
			.header("Authorization", "Basic ".concat(authEncode));
	}
	
	private String encodeAuthorization(String authorization) {
		return Base64.getEncoder().encodeToString(authorization.getBytes());
	}
}
