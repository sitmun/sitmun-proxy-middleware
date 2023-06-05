package org.sitmun.proxy.middleware.request;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.DecoratedRequest;
import org.sitmun.proxy.middleware.decorator.HttpContext;
import org.sitmun.proxy.middleware.decorator.JdbcContext;
import org.sitmun.proxy.middleware.service.ClientService;
import org.springframework.stereotype.Component;

@Component
public class RequestFactory {

  private final ClientService clientService;

  public RequestFactory(ClientService clientService) {
    this.clientService = clientService;
  }

  public DecoratedRequest create(Context context) {
    if (context instanceof HttpContext) {
      return new HttpRequest(clientService);
    } else if (context instanceof JdbcContext) {
      return new JdbcRequest();
    } else {
      throw new IllegalArgumentException("Payload type not supported");
    }
  }
}
