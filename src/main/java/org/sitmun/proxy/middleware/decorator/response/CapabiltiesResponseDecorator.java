package org.sitmun.proxy.middleware.decorator.response;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.ResponseDecorator;
import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.sitmun.proxy.middleware.response.Response;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CapabiltiesResponseDecorator implements ResponseDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof OgcWmsPayloadDto) {
      OgcWmsPayloadDto ogcWmsPayloadDto = (OgcWmsPayloadDto) context;
      return ogcWmsPayloadDto.getParameters().getOrDefault("REQUEST", "").equalsIgnoreCase("GetCapabilities");
    }
    return false;
  }

  @Override
  public void addBehavior(Object response, Context context) {
    log.info("Adding behavior to response {} of {}", response, context);
    if (context instanceof OgcWmsPayloadDto) {
      OgcWmsPayloadDto ogcWmsPayloadDto = (OgcWmsPayloadDto) context;
      //noinspection unchecked
      Response<byte[]> response1 = (Response<byte[]>) response;
      String s = new String(response1.getBody(), StandardCharsets.UTF_8);
      String output = s.replaceAll(ogcWmsPayloadDto.getUri(), response1.getBaseUrl());
      log.info("Replacement of {} by {} in GetCapabilities response", ogcWmsPayloadDto.getUri(), response1.getBaseUrl());
      response1.setBody(output.getBytes(StandardCharsets.UTF_8));
    }
  }
}
