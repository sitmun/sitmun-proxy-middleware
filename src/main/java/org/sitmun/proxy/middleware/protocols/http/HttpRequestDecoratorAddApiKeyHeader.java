package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestDecoratorAddApiKeyHeader implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext ctx && ctx.getSecurity() != null) {
      Map<String, String> headers = ctx.getSecurity().getHeaders();
      return headers != null && headers.containsKey(HttpSecurityConstants.HEADER_X_API_KEY);
    }
    return false;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    Map<String, String> headers = httpContext.getSecurity().getHeaders();
    if (headers != null && headers.containsKey(HttpSecurityConstants.HEADER_X_API_KEY)) {
      String apiKey = headers.get(HttpSecurityConstants.HEADER_X_API_KEY);
      request.setHeader(HttpSecurityConstants.HEADER_X_API_KEY, apiKey);
    }
  }
}
