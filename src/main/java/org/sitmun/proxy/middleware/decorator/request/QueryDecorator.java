package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.dto.DatasourcePayloadDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.request.GlobalRequest;
import org.springframework.stereotype.Component;

@Component
public class QueryDecorator implements RequestDecorator {

  @Override
  public boolean accept(PayloadDto payload) {
    return payload instanceof DatasourcePayloadDto;
  }

  @Override
  public void apply(GlobalRequest globalRequest, PayloadDto payload) {
    globalRequest.getJdbcRequest().setSql(((DatasourcePayloadDto) payload).getSql());
  }

}
