package org.sitmun.proxy.middleware.protocols.jdbc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcRequestDecoratorAddQueryTest {

  @Test
  @DisplayName("addBehavior sets SQL and parameters on executor")
  void addBehaviorSetsSqlAndParametersOnExecutor() {
    JdbcRequestDecoratorAddQuery decorator = new JdbcRequestDecoratorAddQuery();
    JdbcRequestExecutor executor = new JdbcRequestExecutor();
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .sql("SELECT * FROM test WHERE id=?")
            .parameters(List.of("123"))
            .build();

    decorator.addBehavior(executor, payload);

    String describe = executor.describe();
    assertTrue(describe.contains("SELECT * FROM test WHERE id=?"));
    assertTrue(describe.contains("parameters=1"));
  }

  @Test
  @DisplayName("accept returns true for jdbc contexts")
  void acceptReturnsTrueForJdbcContexts() {
    JdbcRequestDecoratorAddQuery decorator = new JdbcRequestDecoratorAddQuery();
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT 1").build();
    assertTrue(decorator.accept(new JdbcRequestExecutor(), payload));
  }
}
