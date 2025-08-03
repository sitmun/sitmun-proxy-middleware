package org.sitmun.proxy.middleware.service;

import org.springframework.http.ResponseEntity;

public interface RequestExecutorResponse<T> {
  ResponseEntity<T> asResponseEntity();
}
