package org.sitmun.proxy.middleware.decorator;

import org.springframework.http.ResponseEntity;

public interface DecoratedResponse<T> {
  ResponseEntity<T> asResponseEntity();
}
