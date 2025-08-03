package org.sitmun.proxy.middleware.protocols.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JdbcRequestDecoratorAddConnection implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    return context instanceof JdbcContext;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    JdbcRequestExecutor request = (JdbcRequestExecutor) target;
    JdbcContext jdbcContext = (JdbcContext) context;
    request.setConnection(getConnection(jdbcContext));
  }

  private Connection getConnection(JdbcContext context) {
    // TODO Reuse connections from a connection pool when possible
    Connection connection = null;
    try {
      Class.forName(context.getDriver());
      connection =
          DriverManager.getConnection(context.getUri(), context.getUser(), context.getPassword());
    } catch (SQLException | ClassNotFoundException e) {
      log.error("Error getting connection: {}", e.getMessage(), e);
    }
    return connection;
  }
}
