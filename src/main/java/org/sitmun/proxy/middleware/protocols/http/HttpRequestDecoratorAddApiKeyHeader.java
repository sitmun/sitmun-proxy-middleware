package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestDecoratorAddApiKeyHeader implements RequestDecorator {

  private static final String API_KEY_HEADER = "X-API-Key";

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext ctx && ctx.getSecurity() != null) {
      Map<String, String> headers = ctx.getSecurity().getHeaders();
      return headers != null && headers.containsKey(API_KEY_HEADER);
    }
    return false;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    Map<String, String> headers = httpContext.getSecurity().getHeaders();
    if (headers != null && headers.containsKey(API_KEY_HEADER)) {
      String apiKey = headers.get(API_KEY_HEADER);
      request.setHeader(API_KEY_HEADER, apiKey);
    }
  }
}
