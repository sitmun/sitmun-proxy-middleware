package org.sitmun.proxy.middleware.protocols.jdbc;

import org.sitmun.proxy.middleware.decorator.Context;

public interface JdbcContext extends Context {
  String getDriver();

  String getUri();

  String getUser();

  String getPassword();

  String getSql();
}
