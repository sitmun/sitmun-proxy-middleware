package org.sitmun.proxy.middleware.protocols.jdbc;

import java.sql.*;
import java.util.*;
import java.util.Date;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.dto.ErrorResponseDto;
import org.sitmun.proxy.middleware.service.RequestExecutor;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;
import org.sitmun.proxy.middleware.service.RequestExecutorResponseImpl;

@Setter
@Slf4j
public class JdbcRequestExecutor implements RequestExecutor {

  private Connection connection;
  private String sql;

  @SuppressWarnings("unchecked")
  @Override
  public RequestExecutorResponse<?> execute() {
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection connectionUsed = connection) {
      executeStatement(connectionUsed, result);
    } catch (SQLException e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      return new RequestExecutorResponseImpl<>(
          null,
          500,
          "application/json",
          new ErrorResponseDto(500, "SQLError", e.getMessage(), "", new Date()));
    }
    return new RequestExecutorResponseImpl<>(null, 200, "application/json", result);
  }

  private void executeStatement(Connection connection, List<Map<String, Object>> result) {
    try (Statement stmt = connection.createStatement()) {
      retrieveResultSetMetadata(stmt, result);
    } catch (SQLException e) {
      log.error("Error in connection: {}", e.getMessage(), e);
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
      log.error("Error in statement: {}", e.getMessage(), e);
    }
  }

  public String describe() {
    return "JdbcRequest{" + "connection=" + connection + ", sql='" + sql + '\'' + '}';
  }
}
