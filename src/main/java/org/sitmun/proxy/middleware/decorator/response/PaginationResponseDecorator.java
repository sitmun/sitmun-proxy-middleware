package org.sitmun.proxy.middleware.decorator.response;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.ResponseDecorator;
import org.springframework.stereotype.Component;

@Component
public class PaginationResponseDecorator implements ResponseDecorator {

  @Override
  public boolean accept(Object target, Context response) {
    // TODO return MediaType.APPLICATION_JSON.equals(response.getHeaders().getContentType());
    return false;
  }

  @Override
  public void addBehavior(Object response, Context context) {
    //TODO  implementation if necessary
  }

}
