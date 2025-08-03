package org.sitmun.proxy.middleware.protocols.http;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestDecoratorAddEndpoint implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext httpContext) {
      return httpContext.getParameters() != null && !httpContext.getParameters().isEmpty();
    } else {
      return false;
    }
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    request.setUrl(httpContext.getUri());
    request.setParameters(httpContext.getParameters());
  }
}
