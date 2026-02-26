package org.sitmun.proxy.middleware.protocols.jdbc;

import java.sql.*;
import java.util.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.dto.ProblemDetail;
import org.sitmun.proxy.middleware.dto.ProblemTypes;
import org.sitmun.proxy.middleware.service.RequestExecutor;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;
import org.sitmun.proxy.middleware.service.RequestExecutorResponseImpl;

@Setter
@Slf4j
public class JdbcRequestExecutor implements RequestExecutor {

  private Connection connection;
  private String sql;
  private List<String> parameters;

  @SuppressWarnings("unchecked")
  @Override
  public RequestExecutorResponse<?> execute() {
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection connectionUsed = connection) {
      executeStatement(connectionUsed, result);
    } catch (SQLException e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      ProblemDetail problem =
          ProblemDetail.builder()
              .type(ProblemTypes.PROXY_SERVICE_ERROR)
              .status(500)
              .title("SQL Error")
              .detail(e.getMessage())
              .instance("")
              .build();
      return new RequestExecutorResponseImpl<>(null, 500, "application/problem+json", problem);
    }
    return new RequestExecutorResponseImpl<>(null, 200, "application/json", result);
  }

  private void executeStatement(Connection connection, List<Map<String, Object>> result)
      throws SQLException {
    if (parameters == null || parameters.isEmpty()) {
      try (Statement stmt = connection.createStatement();
          ResultSet resultSet = stmt.executeQuery(sql)) {
        retrieveResultSetMetadata(resultSet, result);
      }
      return;
    }

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.size(); i++) {
        stmt.setString(i + 1, parameters.get(i));
      }
      try (ResultSet resultSet = stmt.executeQuery()) {
        retrieveResultSetMetadata(resultSet, result);
      }
    }
  }

  private void retrieveResultSetMetadata(ResultSet resultSet, List<Map<String, Object>> result) {
    try {
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
    return "JdbcRequest{"
        + "connection="
        + connection
        + ", sql='"
        + sql
        + '\''
        + ", parameters="
        + (parameters != null ? parameters.size() : 0)
        + '}';
  }
}
