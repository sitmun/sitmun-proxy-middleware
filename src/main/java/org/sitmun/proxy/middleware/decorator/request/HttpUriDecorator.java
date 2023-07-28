package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.HttpContext;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.sitmun.proxy.middleware.request.HttpRequest;
import org.springframework.stereotype.Component;

@Component
public class HttpUriDecorator implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext) {
      HttpContext httpContext = (HttpContext) context;
      return httpContext.getParameters() != null && !httpContext.getParameters().isEmpty();
    } else {
      return false;
    }
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequest request = (HttpRequest) target;
    HttpContext httpContext = (HttpContext) context;
    request.setUrl(httpContext.getUri());
    request.setParameters(httpContext.getParameters());
  }

}
