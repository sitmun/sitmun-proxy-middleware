package org.sitmun.proxy.middleware.decorator;


public interface JdbcContext extends Context {
  String getDriver();
  String getUri();
  String getUser();
  String getPassword();

  String getSql();
}
