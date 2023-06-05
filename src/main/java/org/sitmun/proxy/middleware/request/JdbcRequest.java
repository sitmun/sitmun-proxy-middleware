package org.sitmun.proxy.middleware.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Connection;

@NoArgsConstructor
@Setter
@Getter
public class JdbcRequest {

  private Connection connection;

  private String sql;
}
