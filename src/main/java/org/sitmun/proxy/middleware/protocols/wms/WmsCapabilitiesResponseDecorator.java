package org.sitmun.proxy.middleware.protocols.wms;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.ResponseDecorator;
import org.sitmun.proxy.middleware.service.RequestExecutorResponseImpl;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WmsCapabilitiesResponseDecorator implements ResponseDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof WmsPayloadDto wmsPayloadDto) {
      return wmsPayloadDto
          .getParameters()
          .getOrDefault("REQUEST", "")
          .equalsIgnoreCase("GetCapabilities");
    }
    return false;
  }

  @Override
  public void addBehavior(Object response, Context context) {
    log.info("Adding behavior to response {} of {}", response, context);
    if (context instanceof WmsPayloadDto wmsPayloadDto) {
      //noinspection unchecked
      RequestExecutorResponseImpl<byte[]> requestExecutionResponseImpl1 =
          (RequestExecutorResponseImpl<byte[]>) response;
      String s = new String(requestExecutionResponseImpl1.getBody(), StandardCharsets.UTF_8);
      String output =
          s.replaceAll(wmsPayloadDto.getUri(), requestExecutionResponseImpl1.getBaseUrl());
      log.info(
          "Replacement of {} by {} in GetCapabilities response",
          wmsPayloadDto.getUri(),
          requestExecutionResponseImpl1.getBaseUrl());
      requestExecutionResponseImpl1.setBody(output.getBytes(StandardCharsets.UTF_8));
    }
  }
}
