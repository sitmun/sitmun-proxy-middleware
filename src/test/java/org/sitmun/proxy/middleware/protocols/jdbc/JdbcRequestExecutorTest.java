package org.sitmun.proxy.middleware.protocols.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.dto.ProblemDetail;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class JdbcRequestExecutorTest {

  @Mock private Connection connection;
  @Mock private Statement statement;

  @Test
  void executionFailureOmitsConnectionSqlParametersAndDatabaseError() throws SQLException {
    var executor = new JdbcRequestExecutor();
    executor.setConnection(connection);
    executor.setSql("select secret_value from private_table where tenant = ?");
    executor.setParameters(List.of("tenant-secret"));
    when(connection.prepareStatement(executorSql())).thenThrow(hostileFailure());

    List<ILoggingEvent> events = captureLogs(() -> assertSanitizedFailure(executor));

    assertThat(events)
        .extracting(ILoggingEvent::getFormattedMessage)
        .anySatisfy(
            message ->
                assertThat(message)
                    .contains("SQLException")
                    .doesNotContain(
                        "internal.example",
                        "database-secret",
                        "select secret_value",
                        "tenant-secret"));
    assertThat(events)
        .allSatisfy(event -> assertThat(event.getThrowableProxy()).as("stack trace").isNull());
  }

  @Test
  void descriptionRetainsPresenceAndCountWithoutSensitiveValues() {
    var executor = new JdbcRequestExecutor();
    executor.setConnection(connection);
    executor.setSql("select password from users");
    executor.setParameters(List.of("first-secret", "second-secret"));

    assertThat(executor.describe())
        .contains("connectionPresent=true", "sqlPresent=true", "parameterCount=2")
        .doesNotContain("internal.example", "select password", "first-secret", "second-secret");
  }

  private String executorSql() {
    return "select secret_value from private_table where tenant = ?";
  }

  private SQLException hostileFailure() {
    return new SQLException(
        "Connection jdbc:postgresql://internal.example/private failed for database-secret");
  }

  private static void assertSanitizedFailure(JdbcRequestExecutor executor) {
    var response = executor.execute().asResponseEntity();

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    var problem = (ProblemDetail) response.getBody();
    assertThat(problem.getDetail()).isEqualTo("The JDBC service request failed");
    assertThat(problem.getInstance()).isEqualTo("/proxy");
    assertThat(problem.getProperties()).containsEntry("origin", "upstream-service");
    assertThat(problem.toString())
        .doesNotContain(
            "internal.example", "database-secret", "select secret_value", "tenant-secret");
  }

  private static List<ILoggingEvent> captureLogs(Runnable action) {
    Logger logger = (Logger) LoggerFactory.getLogger(JdbcRequestExecutor.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    Level previousLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    try {
      action.run();
      return List.copyOf(appender.list);
    } finally {
      logger.setLevel(previousLevel);
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  @DisplayName("execute uses Statement fallback when parameters are absent")
  void executeUsesStatementFallbackWhenParametersAbsent() throws Exception {
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

    var responseEntity = executor.execute().asResponseEntity();

    assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> body = (List<Map<String, Object>>) responseEntity.getBody();
    assertThat(body).hasSize(1);
    assertThat(body.get(0)).containsEntry("value", 1);

    verify(connection).createStatement();
    verify(statement).executeQuery("SELECT 1");
    verify(connection, never()).prepareStatement(anyString());
  }

  @Test
  @DisplayName("execute uses PreparedStatement when parameters are present")
  void executeUsesPreparedStatementWhenParametersPresent() throws Exception {
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

    var responseEntity = executor.execute().asResponseEntity();

    assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> body = (List<Map<String, Object>>) responseEntity.getBody();
    assertThat(body).hasSize(1);
    assertThat(body.get(0)).containsEntry("value", "ok");

    verify(connection).prepareStatement("SELECT * FROM test WHERE a=? AND b=?");
    verify(preparedStatement).setString(1, "x");
    verify(preparedStatement).setString(2, "y");
    verify(preparedStatement).executeQuery();
    verify(connection, never()).createStatement();
  }
}
