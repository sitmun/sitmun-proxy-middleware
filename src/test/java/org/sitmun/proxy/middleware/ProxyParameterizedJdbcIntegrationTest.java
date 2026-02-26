package org.sitmun.proxy.middleware;

import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY;
import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.TYPE_SQL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.test.fixtures.AuthorizationProxyFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DisplayName("Proxy integration test with mock backend and parameterized JDBC")
class ProxyParameterizedJdbcIntegrationTest {

  @Autowired private MockMvc mvc;

  @Autowired private RestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Value("${sitmun.backend.config.url}")
  private String configUrl;

  @Value("${sitmun.backend.config.secret}")
  private String secret;

  private MockRestServiceServer mockBackend;

  @BeforeEach
  void setUp() {
    mockBackend = MockRestServiceServer.createServer(restTemplate);
  }

  @AfterEach
  void tearDown() {
    mockBackend.verify();
  }

  @Test
  @DisplayName("GET proxy with mock backend returns parameterized JDBC result")
  void getProxyWithMockBackendReturnsParameterizedJdbcResult() throws Exception {
    // Given: Mock backend returns a ConfigProxyDto with parameterized JDBC payload
    ConfigProxyDto configResponse =
        ConfigProxyDto.builder()
            .type(TYPE_SQL)
            .exp(System.currentTimeMillis() + 3600000) // 1 hour from now
            .payload(AuthorizationProxyFixtures.inMemoryH2DatabaseWithParameters())
            .build();

    String responseJson = objectMapper.writeValueAsString(configResponse);

    mockBackend
        .expect(requestTo(configUrl))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(PROXY_MIDDLEWARE_KEY, secret))
        .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

    // When: Call the proxy endpoint
    mvc.perform(get("/proxy/1/1/SQL/23"))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].ID").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].A").value("x"));
  }

  @Test
  @DisplayName("GET proxy with mock backend uses Statement when no parameters")
  void getProxyWithMockBackendUsesStatementWhenNoParameters() throws Exception {
    // Given: Mock backend returns a ConfigProxyDto with JDBC payload WITHOUT parameters
    ConfigProxyDto configResponse =
        ConfigProxyDto.builder()
            .type(TYPE_SQL)
            .exp(System.currentTimeMillis() + 3600000)
            .payload(AuthorizationProxyFixtures.inMemoryH2DatabaseWithoutParameters())
            .build();

    String responseJson = objectMapper.writeValueAsString(configResponse);

    mockBackend
        .expect(requestTo(configUrl))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(PROXY_MIDDLEWARE_KEY, secret))
        .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

    // When: Call the proxy endpoint
    mvc.perform(get("/proxy/1/1/SQL/23"))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].ID").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].A").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$[1].ID").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$[1].A").exists());
  }

  @Test
  @DisplayName("GET proxy returns 401 when backend rejects authorization")
  void getProxyReturns401WhenBackendRejectsAuthorization() throws Exception {
    // Given: Mock backend returns 401 Unauthorized
    mockBackend
        .expect(requestTo(configUrl))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(PROXY_MIDDLEWARE_KEY, secret))
        .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withUnauthorizedRequest());

    // When: Call the proxy endpoint
    mvc.perform(get("/proxy/1/1/SQL/23"))
        .andExpect(status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
        .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Backend Error"));
  }
}
