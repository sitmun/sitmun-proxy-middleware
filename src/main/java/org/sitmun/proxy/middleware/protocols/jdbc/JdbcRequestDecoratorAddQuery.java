package org.sitmun.proxy.middleware.protocols.jdbc;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;

@Component
public class JdbcRequestDecoratorAddQuery implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    return context instanceof JdbcContext;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    JdbcRequestExecutor request = (JdbcRequestExecutor) target;
    JdbcContext jdbcContext = (JdbcContext) context;
    request.setSql(jdbcContext.getSql());
  }
}
