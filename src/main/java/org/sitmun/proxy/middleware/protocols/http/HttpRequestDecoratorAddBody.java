package org.sitmun.proxy.middleware.protocols.http;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestDecoratorAddBody implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof WmsPayloadDto wmsPayload) {
      return wmsPayload.getMethod().equals("POST") && wmsPayload.getBody() != null;
    } else {
      return false;
    }
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    WmsPayloadDto wmsPayload = (WmsPayloadDto) context;

    request.setBody(wmsPayload.getBody());
    request.setHeader("Content-Type", "application/xml");
  }
}
