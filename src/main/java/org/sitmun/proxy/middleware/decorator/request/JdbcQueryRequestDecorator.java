package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.JdbcContext;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.sitmun.proxy.middleware.request.JdbcRequest;
import org.springframework.stereotype.Component;

@Component
public class JdbcQueryRequestDecorator implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    return context instanceof JdbcContext;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    JdbcRequest request = (JdbcRequest) target;
    JdbcContext jdbcContext = (JdbcContext) context;
    request.setSql(jdbcContext.getSql());
  }

}
