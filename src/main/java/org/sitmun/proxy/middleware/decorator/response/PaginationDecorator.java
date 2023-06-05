package org.sitmun.proxy.middleware.decorator.response;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class PaginationDecorator implements ResponseDecorator {

  @Override
  public boolean accept(ResponseEntity<?> response) {
    // TODO return MediaType.APPLICATION_JSON.equals(response.getHeaders().getContentType());
    return false;
  }

  @Override
  public void apply(ResponseEntity<?> response) {
    //TODO  implementation if necesary
  }

}
