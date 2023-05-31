package org.sitmun.proxy.middleware.service;

import org.sitmun.proxy.middleware.decorator.request.RequestDecorator;
import org.sitmun.proxy.middleware.decorator.response.ResponseDecorator;
import org.sitmun.proxy.middleware.dto.DatasourcePayloadDto;
import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.request.GlobalRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalRequestService {

  @Autowired
  private List<RequestDecorator> requestDecorators;

  @Autowired
  private List<ResponseDecorator> responseDecorators;
  
  @Autowired
  private ClientService clientService;

  public ResponseEntity<?> executeRequest(PayloadDto payload) {
    GlobalRequest globalRequest = new GlobalRequest();
    if (payload instanceof OgcWmsPayloadDto) {
      globalRequest.setMapServiceRequest(clientService);
      globalRequest.getCustomHttpRequest().getRequestBuilder()
        .url(((OgcWmsPayloadDto) payload).getUri());
    } else if (payload instanceof DatasourcePayloadDto) {
      globalRequest.setDatabaseRequest();
    }
    applyRequestDecorators(globalRequest, payload);
    ResponseEntity<?> response = globalRequest.execute();
    applyResponseDecorators(response);
    return response;
  }

  private void applyRequestDecorators(GlobalRequest globalRequest, PayloadDto payload) {
    requestDecorators.forEach(d -> {
      if (d.accept(payload)) {
        d.apply(globalRequest, payload);
      }
    });
  }

  private void applyResponseDecorators(ResponseEntity<?> response) {
    responseDecorators.forEach(d -> {
      if (d.accept(response)) {
        d.apply(response);
      }
    });
  }
}
