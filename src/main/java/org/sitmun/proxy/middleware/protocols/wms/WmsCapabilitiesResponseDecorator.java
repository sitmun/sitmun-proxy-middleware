package org.sitmun.proxy.middleware.protocols.wms;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
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
      String fullUri = wmsPayloadDto.getUri();
      String baseUri = fullUri.split("\\?")[0];
      
      log.debug("WMS PAYLOAD FULL URI: {}", fullUri);
      log.debug("WMS PAYLOAD BASE URI: {}", baseUri);
      log.debug("REQ EXEC RES URL: {}", requestExecutionResponseImpl1.getBaseUrl());
      
      String baseUrl = requestExecutionResponseImpl1.getBaseUrl();

      String servicePath = baseUri.replaceAll("/(?:wms|wfs|wcs|ows)/?$", "");
      
      log.info("GEOSERVER PATH: {}", servicePath);

      Pattern pattern = Pattern.compile(
          Pattern.quote(servicePath) + "(?:/(?:wms|wfs|wcs|ows))?(?=[?'\"]|\\s|$)",
          Pattern.CASE_INSENSITIVE);
      String output = pattern.matcher(s).replaceAll(baseUrl);
      
      log.info(
          "Replacement of {} by {} in GetCapabilities response",
          servicePath,
          baseUrl);
      requestExecutionResponseImpl1.setBody(output.getBytes(StandardCharsets.UTF_8));
    }
  }
}
