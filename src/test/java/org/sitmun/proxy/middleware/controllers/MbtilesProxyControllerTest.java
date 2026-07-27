package org.sitmun.proxy.middleware.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.mbtiles.JwtPrincipalExtractor;
import org.sitmun.proxy.middleware.mbtiles.MbtilesConfigClient;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.BackendConfigRequest;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.BackendConfigResponse;
import org.sitmun.proxy.middleware.mbtiles.MbtilesJobHandleService;
import org.sitmun.proxy.middleware.mbtiles.MbtilesJobHandleService.JobHandleClaims;
import org.sitmun.proxy.middleware.mbtiles.MbtilesProperties;
import org.sitmun.proxy.middleware.mbtiles.MbtilesProxyService;
import org.sitmun.proxy.middleware.mbtiles.MbtilesRequestValidator;
import org.sitmun.proxy.middleware.mbtiles.MbtilesUpstreamClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MbtilesProxyController.class)
@Import({
  MbtilesProxyService.class,
  MbtilesRequestValidator.class,
  MbtilesJobHandleService.class,
  JwtPrincipalExtractor.class,
  MbtilesProxyControllerTest.TestBeans.class
})
@DisplayName("MBTiles proxy routes")
class MbtilesProxyControllerTest {

  private static final String BODY =
      """
      {
        "bbox": {"minX":0,"minY":0,"maxX":1,"maxY":1},
        "minZoom": 0,
        "maxZoom": 4,
        "srs": "EPSG:25831",
        "services": [{"serviceId":5,"layerIds":[7]}]
      }
      """;

  @Autowired private MockMvc mvc;
  @Autowired private MbtilesJobHandleService jobHandleService;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private MbtilesConfigClient configClient;
  @MockitoBean private MbtilesUpstreamClient upstreamClient;

  @TestConfiguration
  static class TestBeans {
    @Bean
    MbtilesProperties mbtilesProperties() {
      return new MbtilesProperties(
          "http://mbtiles.test/mbtiles",
          "test-only-insecure-mbtiles-job-handle-secret",
          Duration.ofHours(1),
          65536,
          12,
          List.of("EPSG:4326", "EPSG:3857", "EPSG:25831"),
          Duration.ofSeconds(2),
          Duration.ofSeconds(5));
    }

    @Bean
    Clock clock() {
      return Clock.systemUTC();
    }
  }

  @Test
  @DisplayName("Estimate requires Bearer and forwards canonical tile request only")
  void estimateRequiresBearerAndForwardsCanonicalBody() throws Exception {
    Map<String, Object> canonical = Map.of("mapServices", List.of());
    when(configClient.authorize(any(), eq("mobile-token")))
        .thenReturn(
            new BackendConfigResponse(10, 20, "estimate", "alice", 1_700_000_000L, canonical));
    when(upstreamClient.postEstimate(any())).thenReturn("{\"tileCount\":42}");

    mvc.perform(
            post("/proxy/10/20/mbtiles/estimate")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer mobile-token")
                .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tileCount").value(42));

    var captor = org.mockito.ArgumentCaptor.forClass(BackendConfigRequest.class);
    verify(configClient).authorize(captor.capture(), eq("mobile-token"));
    assertThat(captor.getValue().action()).isEqualTo("estimate");
    assertThat(captor.getValue().services()).isNotEmpty();
    verify(upstreamClient).postEstimate(canonical);
  }

  @Test
  @DisplayName("Create returns opaque job handle, not bare MBTiles job id")
  void createReturnsOpaqueHandle() throws Exception {
    when(configClient.authorize(any(), eq("mobile-token")))
        .thenReturn(new BackendConfigResponse(10, 20, "create", "alice", 1L, Map.of("ok", true)));
    when(upstreamClient.postCreate(any())).thenReturn("99");

    String response =
        mvc.perform(
                post("/proxy/10/20/mbtiles")
                    .contentType(APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mobile-token")
                    .content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobHandle").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String handle = objectMapper.readTree(response).path("jobHandle").asText();
    assertThat(handle).doesNotContain("99");
    assertThat(jobHandleService.verify(handle, "alice", 10, 20))
        .isPresent()
        .get()
        .extracting(JobHandleClaims::jobId)
        .isEqualTo(99L);
  }

  @Test
  @DisplayName("Missing Bearer on MBTiles routes returns 401")
  void missingBearerReturnsUnauthorized() throws Exception {
    mvc.perform(post("/proxy/10/20/mbtiles/estimate").contentType(APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(configClient, upstreamClient);
  }

  @Test
  @DisplayName("Rejects oversized JSON before backend or MBTiles calls")
  void rejectsOversizedJson() throws Exception {
    String oversized =
        "{\"bbox\":{\"minX\":0,\"minY\":0,\"maxX\":1,\"maxY\":1},\"minZoom\":0,\"maxZoom\":1,"
            + "\"srs\":\"EPSG:25831\",\"services\":[{\"serviceId\":1,\"layerIds\":[1]}],\"pad\":\""
            + "x".repeat(70_000)
            + "\"}";

    mvc.perform(
            post("/proxy/10/20/mbtiles/estimate")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer mobile-token")
                .content(oversized))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(configClient, upstreamClient);
  }

  @Test
  @DisplayName("Status verifies handle and calls MBTiles with internal job id")
  void statusVerifiesHandleAndCallsUpstream() throws Exception {
    String handle = jobHandleService.mint("alice", 10, 20, 77L);
    when(configClient.authorize(any(), eq(jwtWithSub("alice"))))
        .thenReturn(new BackendConfigResponse(10, 20, "status", "alice", 1L, null));
    when(upstreamClient.getStatus(77L)).thenReturn("{\"status\":\"RUNNING\"}");

    mvc.perform(
            get("/proxy/10/20/mbtiles/" + handle)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtWithSub("alice")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNNING"));

    verify(upstreamClient).getStatus(77L);
  }

  @Test
  @DisplayName("Invalid SRS is rejected before upstream calls")
  void rejectsUnsupportedSrs() throws Exception {
    String badSrs =
        """
        {
          "bbox": {"minX":0,"minY":0,"maxX":1,"maxY":1},
          "minZoom": 0,
          "maxZoom": 1,
          "srs": "EPSG:99999",
          "services": [{"serviceId":5,"layerIds":[7]}]
        }
        """;

    mvc.perform(
            post("/proxy/10/20/mbtiles")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer mobile-token")
                .content(badSrs))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(configClient, upstreamClient);
  }

  private static String jwtWithSub(String sub) {
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(("{\"sub\":\"" + sub + "\"}").getBytes(StandardCharsets.UTF_8));
    return "eyJhbGciOiJub25lIn0." + payload + ".x";
  }
}
