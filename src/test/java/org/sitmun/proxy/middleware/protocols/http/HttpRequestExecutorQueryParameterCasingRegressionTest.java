package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regression tests for desired query parameter <strong>name</strong> casing on upstream URLs.
 *
 * <p>These assert that {@link HttpRequestExecutor} does not rewrite keys to uppercase when merging
 * the configured URL with the parameter map. They fail until {@code
 * rebuildUrlWithMergedQueryParams} preserves casing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestExecutor query parameter name casing (regression)")
class HttpRequestExecutorQueryParameterCasingRegressionTest {

  @Mock private HttpClient httpClient;

  private HttpRequestExecutor httpRequestExecutor;

  @BeforeEach
  void setUp() {
    httpRequestExecutor = new HttpRequestExecutor("https://api.example.com", httpClient);
  }

  @Test
  @DisplayName("Parameter map keys keep lowercase in generated query string")
  void parameterMapKeysKeepLowercaseInQueryString() {
    httpRequestExecutor.setUrl("https://api.example.com/search");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("query", "test");
    parameters.put("limit", "10");
    httpRequestExecutor.setParameters(parameters);

    String result = httpRequestExecutor.getUrl();

    assertThat(result)
        .contains("query=test")
        .contains("limit=10")
        .doesNotContain("QUERY=")
        .doesNotContain("LIMIT=");
  }

  @Test
  @DisplayName("Existing query keys in the base URL keep their original casing")
  void existingQueryKeysKeepOriginalCasing() {
    httpRequestExecutor.setUrl("https://api.example.com/search?existing=value");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("pageSize", "25");
    httpRequestExecutor.setParameters(parameters);

    String result = httpRequestExecutor.getUrl();

    assertThat(result)
        .contains("existing=value")
        .contains("pageSize=25")
        .doesNotContain("EXISTING=")
        .doesNotContain("PAGESIZE=");
  }

  @Test
  @DisplayName("Keys that differ only by case remain distinct in the query string")
  void keysDifferingOnlyByCaseRemainDistinct() {
    httpRequestExecutor.setUrl("https://api.example.com/search");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("param", "value1");
    parameters.put("PARAM", "value2");
    httpRequestExecutor.setParameters(parameters);

    String result = httpRequestExecutor.getUrl();

    assertThat(result).contains("param=value1").contains("PARAM=value2");
  }

  @Test
  @DisplayName("After template expansion, leftover query keys keep requested casing")
  void leftoverQueryKeysKeepCasingAfterTemplateExpansion() {
    httpRequestExecutor.setUrl("https://api.example.com/users/{id}");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("id", "42");
    parameters.put("limit", "10");
    httpRequestExecutor.setParameters(parameters);

    String result = httpRequestExecutor.getUrl();

    assertThat(result)
        .contains("limit=10")
        .doesNotContain("LIMIT=")
        .startsWith("https://api.example.com/users/42");
  }
}
