package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.dto.DatasourcePayloadDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.request.GlobalRequest;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class ConnectionDecorator implements RequestDecorator {

  @Override
  public boolean accept(PayloadDto payload) {
    return payload instanceof DatasourcePayloadDto;
  }

  @Override
  public void apply(GlobalRequest globalRequest, PayloadDto payload) {
    globalRequest.getJdbcRequest().setConnection(getConnection((DatasourcePayloadDto) payload));
  }

  private Connection getConnection(DatasourcePayloadDto datasourcePayload) {
    Connection connection = null;
    try {
      Class.forName(datasourcePayload.getDriver());
      connection = DriverManager.getConnection(datasourcePayload.getUri(), datasourcePayload.getUser(), datasourcePayload.getPassword());
    } catch (SQLException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return connection;
  }
}
