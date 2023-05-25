package org.sitmun.proxy.middleware.decorator.response;

import org.springframework.http.ResponseEntity;

public interface ResponseDecorator {

  boolean accept(ResponseEntity<?> response);

  void apply(ResponseEntity<?> response);
}
