package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.JdbcContext;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.sitmun.proxy.middleware.request.JdbcRequest;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class JdbcConnectionRequestDecorator implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    return context instanceof JdbcContext;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    JdbcRequest request = (JdbcRequest) target;
    JdbcContext jdbcContext = (JdbcContext) context;
    request.setConnection(getConnection(jdbcContext));
  }

  private Connection getConnection(JdbcContext context) {
    // TODO Reuse connections from a connection pool when possible
    Connection connection = null;
    try {
      Class.forName(context.getDriver());
      connection = DriverManager.getConnection(context.getUri(), context.getUser(), context.getPassword());
    } catch (SQLException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return connection;
  }
}
