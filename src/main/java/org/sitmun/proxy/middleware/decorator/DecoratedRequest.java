package org.sitmun.proxy.middleware.decorator;

public interface DecoratedRequest {
  <T> DecoratedResponse<T> execute();
}
