package org.sitmun.proxy.middleware.service;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.protocols.http.HttpClient;
import org.sitmun.proxy.middleware.protocols.http.HttpContext;
import org.sitmun.proxy.middleware.protocols.http.HttpRequestExecutor;
import org.sitmun.proxy.middleware.protocols.jdbc.JdbcContext;
import org.sitmun.proxy.middleware.protocols.jdbc.JdbcRequestExecutor;
import org.springframework.stereotype.Component;

@Component
public class RequestExecutorFactory {

  private final HttpClient httpClient;

  public RequestExecutorFactory(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public RequestExecutor create(String baseUrl, Context context) {
    if (context instanceof HttpContext) {
      return new HttpRequestExecutor(baseUrl, httpClient);
    } else if (context instanceof JdbcContext) {
      return new JdbcRequestExecutor();
    } else {
      throw new IllegalArgumentException("Payload type not supported");
    }
  }
}
