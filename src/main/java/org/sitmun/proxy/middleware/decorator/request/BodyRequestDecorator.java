package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.HttpContext;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.sitmun.proxy.middleware.request.HttpRequest;
import org.springframework.stereotype.Component;

@Component
public class BodyRequestDecorator implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    // TODO complete implementation
    // OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;
    // result = "POST".equalsIgnoreCase(ogcPayload.getMethod()) && ogcPayload.getRequestBody() != null;
    return context instanceof HttpContext;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequest request = (HttpRequest) target;
    HttpContext httpContext = (HttpContext) context;
    // TODO Valid implementation
    //Example
/*		OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;
		globalRequest.getCustomHttpRequest().getRequestBuilder()
			.post(ogcPayload.getRequestBody());*/
  }
}
