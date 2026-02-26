package org.sitmun.proxy.middleware.protocols.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;
import org.springframework.http.ResponseEntity;

class JdbcRequestExecutorTest {

  @Test
  @DisplayName("execute uses Statement fallback when parameters are absent")
  void executeUsesStatementFallbackWhenParametersAbsent() throws Exception {
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);
    ResultSetMetaData metadata = mock(ResultSetMetaData.class);

    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
    when(resultSet.getMetaData()).thenReturn(metadata);
    when(metadata.getColumnCount()).thenReturn(1);
    when(metadata.getColumnLabel(1)).thenReturn("value");
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getObject(1)).thenReturn(1);

    JdbcRequestExecutor executor = new JdbcRequestExecutor();
    executor.setConnection(connection);
    executor.setSql("SELECT 1");

    RequestExecutorResponse<?> response = executor.execute();
    ResponseEntity<?> responseEntity = response.asResponseEntity();

    assertNotNull(response);
    assertEquals(200, responseEntity.getStatusCode().value());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> body = (List<Map<String, Object>>) responseEntity.getBody();
    assertEquals(1, body.size());
    assertEquals(1, body.get(0).get("value"));

    verify(connection).createStatement();
    verify(statement).executeQuery("SELECT 1");
    verify(connection, never()).prepareStatement(anyString());
  }

  @Test
  @DisplayName("execute uses PreparedStatement when parameters are present")
  void executeUsesPreparedStatementWhenParametersPresent() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    ResultSetMetaData metadata = mock(ResultSetMetaData.class);

    when(connection.prepareStatement("SELECT * FROM test WHERE a=? AND b=?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.getMetaData()).thenReturn(metadata);
    when(metadata.getColumnCount()).thenReturn(1);
    when(metadata.getColumnLabel(1)).thenReturn("value");
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getObject(1)).thenReturn("ok");

    JdbcRequestExecutor executor = new JdbcRequestExecutor();
    executor.setConnection(connection);
    executor.setSql("SELECT * FROM test WHERE a=? AND b=?");
    executor.setParameters(List.of("x", "y"));

    RequestExecutorResponse<?> response = executor.execute();
    ResponseEntity<?> responseEntity = response.asResponseEntity();

    assertNotNull(response);
    assertEquals(200, responseEntity.getStatusCode().value());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> body = (List<Map<String, Object>>) responseEntity.getBody();
    assertEquals(1, body.size());
    assertEquals("ok", body.get(0).get("value"));

    verify(connection).prepareStatement("SELECT * FROM test WHERE a=? AND b=?");
    verify(preparedStatement).setString(1, "x");
    verify(preparedStatement).setString(2, "y");
    verify(preparedStatement).executeQuery();
    verify(connection, never()).createStatement();
  }
}
