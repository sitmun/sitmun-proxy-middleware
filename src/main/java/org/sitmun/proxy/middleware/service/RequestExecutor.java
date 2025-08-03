package org.sitmun.proxy.middleware.service;

public interface RequestExecutor {
  <T> RequestExecutorResponse<T> execute();

  String describe();
}
