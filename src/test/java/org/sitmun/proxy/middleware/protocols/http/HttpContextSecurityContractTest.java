package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.HEADER_AUTHORIZATION;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.HEADER_X_API_KEY;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.SCHEME_BASIC;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.TYPE_API_KEY;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.TYPE_HTTP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.HttpSecurityDto;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;

@DisplayName("HttpContextSecurity / HttpSecurityDto contract")
class HttpContextSecurityContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @DisplayName("HttpSecurityDto is HttpContextSecurity and holds http/basic vs apiKey field shapes")
  void httpSecurityDtoImplementsInterfaceContract() {
    HttpSecurityDto basic =
        HttpSecurityDto.builder()
            .type(TYPE_HTTP)
            .scheme(SCHEME_BASIC)
            .username("u")
            .password("p")
            .build();
    assertThat(basic).isInstanceOf(HttpContextSecurity.class);
    assertThat(basic)
        .extracting(
            HttpSecurityDto::getType,
            HttpSecurityDto::getScheme,
            HttpSecurityDto::getUsername,
            HttpSecurityDto::getPassword,
            HttpSecurityDto::getHeaders)
        .containsExactly(TYPE_HTTP, SCHEME_BASIC, "u", "p", null);

    HttpSecurityDto apiKey =
        HttpSecurityDto.builder()
            .type(TYPE_API_KEY)
            .headers(Map.of(HEADER_X_API_KEY, "secret", HEADER_AUTHORIZATION, "Bearer x"))
            .build();
    assertThat(apiKey).isInstanceOf(HttpContextSecurity.class);
    assertThat(apiKey)
        .extracting(
            HttpSecurityDto::getType,
            HttpSecurityDto::getScheme,
            HttpSecurityDto::getUsername,
            HttpSecurityDto::getPassword)
        .containsExactly(TYPE_API_KEY, null, null, null);
    assertThat(apiKey.getHeaders())
        .containsEntry(HEADER_X_API_KEY, "secret")
        .containsEntry(HEADER_AUTHORIZATION, "Bearer x");
  }

  @Test
  @DisplayName("HttpSecurityDto.describeForLog follows OpenAPI type expectations (no secrets)")
  void describeForLogReflectsOpenApiExpectations() {
    assertThat(
            HttpSecurityDto.builder()
                .type(TYPE_API_KEY)
                .headers(java.util.Map.of(HEADER_X_API_KEY, "secret"))
                .build()
                .describeForLog())
        .contains(
            "type=apiKey", "headerNames=[%s]".formatted(HEADER_X_API_KEY), "queryParamNames=[]")
        .doesNotContain("secret");
    assertThat(
            HttpSecurityDto.builder()
                .type(TYPE_API_KEY)
                .headers(java.util.Map.of())
                .build()
                .describeForLog())
        .isEqualTo("type=apiKey, headerNames=[], queryParamNames=[]");
    assertThat(
            HttpSecurityDto.builder()
                .type(TYPE_HTTP)
                .scheme(SCHEME_BASIC)
                .username("alice")
                .password("zqp9x")
                .build()
                .describeForLog())
        .isEqualTo("type=http, scheme=basic, username=alice, password=set, queryParamNames=[]")
        .doesNotContain("zqp9x");
  }

  @Test
  @DisplayName("JSON deserialization matches backend OpenAPI-style security (basic)")
  void deserializesHttpBasicSecurityFromJson() throws JsonProcessingException {
    String json =
        """
        {
          "type": "%s",
          "scheme": "%s",
          "username": "apiUser",
          "password": "apiSecret"
        }
        """
            .formatted(TYPE_HTTP, SCHEME_BASIC);
    HttpSecurityDto dto = objectMapper.readValue(json, HttpSecurityDto.class);
    assertThat(dto.getType()).isEqualTo(TYPE_HTTP);
    assertThat(dto.getScheme()).isEqualTo(SCHEME_BASIC);
    assertThat(dto.getUsername()).isEqualTo("apiUser");
    assertThat(dto.getPassword()).isEqualTo("apiSecret");
  }

  @Test
  @DisplayName("JSON deserialization matches backend OpenAPI-style security (apiKey)")
  void deserializesApiKeySecurityFromJson() throws JsonProcessingException {
    String json =
        """
        {
          "type": "%s",
          "headers": {
            "%s": "secret-key",
            "%s": "Bearer token"
          }
        }
        """
            .formatted(TYPE_API_KEY, HEADER_X_API_KEY, HEADER_AUTHORIZATION);
    HttpSecurityDto dto = objectMapper.readValue(json, HttpSecurityDto.class);
    assertThat(dto.getType()).isEqualTo(TYPE_API_KEY);
    assertThat(dto.getHeaders()).containsEntry(HEADER_X_API_KEY, "secret-key");
    assertThat(dto.getHeaders()).containsEntry(HEADER_AUTHORIZATION, "Bearer token");
  }

  @Test
  @DisplayName("Nested ConfigProxyDto JSON deserializes payload security")
  void deserializesSecurityInsideConfigProxyPayload() throws JsonProcessingException {
    String json =
        """
        {
          "type": "API",
          "exp": 1000,
          "payload": {
            "type": "OgcWmsPayload",
            "vary": [],
            "uri": "https://api.example.com/r",
            "method": "GET",
            "security": {
              "type": "%s",
              "headers": { "X-Custom": "v" }
            }
          }
        }
        """
            .formatted(TYPE_API_KEY);
    ConfigProxyDto dto = objectMapper.readValue(json, ConfigProxyDto.class);
    assertThat(dto.getType()).isEqualTo("API");
    assertThat(dto.getPayload()).isInstanceOf(WmsPayloadDto.class);
    WmsPayloadDto wms = (WmsPayloadDto) dto.getPayload();
    assertThat(wms.getSecurity()).isNotNull();
    assertThat(wms.getSecurity().getType()).isEqualTo(TYPE_API_KEY);
    assertThat(wms.getSecurity().getHeaders()).containsEntry("X-Custom", "v");
  }
}
