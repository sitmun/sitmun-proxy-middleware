package org.sitmun.proxy.middleware.decorator.request;

import okhttp3.Request.Builder;
import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.request.GlobalRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ParametersDecorator implements RequestDecorator {

  @Override
  public boolean accept(PayloadDto payload) {
    boolean result = false;
    if (payload instanceof OgcWmsPayloadDto) {
      OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;
      result = ogcPayload.getParameters() != null && !ogcPayload.getParameters().isEmpty();
    }
    return result;
  }

  @Override
  public void apply(GlobalRequest globalRequest, PayloadDto payload) {
    OgcWmsPayloadDto ogcPayload = (OgcWmsPayloadDto) payload;
    Builder requestBuilder = globalRequest.getCustomHttpRequest().getRequestBuilder();
    StringBuilder uri = new StringBuilder(ogcPayload.getUri().concat("?"));
    StringBuilder params = new StringBuilder();
    Map<String, String> parameters = ogcPayload.getParameters();
    parameters.keySet().forEach(k -> params.append("&").append(k).append("=").append(parameters.get(k)));
    uri.append(params.substring(1));
    requestBuilder.url(uri.toString());
  }

}
