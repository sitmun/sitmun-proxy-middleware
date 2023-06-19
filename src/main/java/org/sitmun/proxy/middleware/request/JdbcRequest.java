package org.sitmun.proxy.middleware.request;


import org.sitmun.proxy.middleware.decorator.DecoratedRequest;
import org.sitmun.proxy.middleware.decorator.DecoratedResponse;
import org.sitmun.proxy.middleware.dto.ErrorResponseDTO;
import org.sitmun.proxy.middleware.response.Response;

import java.sql.*;
import java.util.Date;
import java.util.*;

public class JdbcRequest implements DecoratedRequest {

  private Connection connection;
  private String sql;

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  @Override
  public DecoratedResponse<?> execute() {
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection connectionUsed = connection) {
      executeStatement(connectionUsed, result);
    } catch (SQLException e) {
      e.printStackTrace();
      return new Response<>(500, "application/json", new ErrorResponseDTO(500, "SQLError", e.getMessage(), "", new Date()));
    }
    return new Response<>(200, "application/json", result);
  }

  private void executeStatement(Connection connection, List<Map<String, Object>> result) {
    try (Statement stmt = connection.createStatement()) {
      retrieveResultSetMetadata(stmt, result);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void retrieveResultSetMetadata(Statement stmt, List<Map<String, Object>> result) {
    try (ResultSet resultSet = stmt.executeQuery(sql)) {
      ResultSetMetaData metadata = resultSet.getMetaData();
      while (resultSet.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
          Object value = resultSet.getObject(i);
          row.put(metadata.getColumnLabel(i), value);
        }
        result.add(row);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
