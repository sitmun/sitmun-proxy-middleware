package org.sitmun.proxy.middleware.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;

class PayloadLoggingTest {

  @Test
  void httpPayloadDescriptionOmitsCredentialsUrlsAndValues() {
    var payload =
        WmsPayloadDto.builder()
            .uri("https://internal.example/wms")
            .method("GET")
            .parameters(Map.of("apiKey", "secret-value"))
            .security(
                HttpSecurityDto.builder()
                    .type("http")
                    .scheme("basic")
                    .username("upstream-user")
                    .password("upstream-password")
                    .build())
            .build();

    assertThat(payload.describe())
        .contains("method='GET'", "apiKey", "securityPresent=true")
        .doesNotContain("internal.example", "secret-value", "upstream-user", "upstream-password");
  }

  @Test
  void jdbcPayloadDescriptionOmitsConnectionAndSqlDetails() {
    var payload =
        JdbcPayloadDto.builder()
            .uri("jdbc:postgresql://internal.example/private")
            .user("db-user")
            .password("db-password")
            .driver("org.postgresql.Driver")
            .sql("select secret from private_table")
            .parameters(List.of("tenant-secret"))
            .build();

    assertThat(payload.describe())
        .contains("parameterCount=1")
        .doesNotContain(
            "internal.example",
            "db-user",
            "db-password",
            "org.postgresql.Driver",
            "select secret",
            "tenant-secret");
  }
}
