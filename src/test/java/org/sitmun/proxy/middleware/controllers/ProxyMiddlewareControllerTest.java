package org.sitmun.proxy.middleware.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.service.RequestConfigurationService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProxyMiddlewareController URL decode and forwarding tests")
class ProxyMiddlewareControllerTest {

  @Mock private RequestConfigurationService requestConfigurationService;

  @Mock private HttpServletRequest httpServletRequest;

  @InjectMocks private ProxyMiddlewareController proxyMiddlewareController;

  @Test
  @DisplayName(
      "GET: URL returned to client can be decoded and forwarded to backend for access validation")
  void getServiceDecodesUrlAndForwardsToBackend() {
    // Given: A client requests a proxy URL with path parameters and query parameters
    Integer appId = 1;
    Integer terId = 2;
    String type = "WMS";
    Integer typeId = 100;
    String token = "Bearer test-jwt-token";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("SERVICE", "WMS");
    queryParams.put("VERSION", "1.3.0");
    queryParams.put("REQUEST", "GetMap");
    queryParams.put("LAYERS", "test:layer");

    String requestUrl = "http://localhost:8080/proxy/1/2/WMS/100";
    when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(requestUrl));

    doReturn(ResponseEntity.ok("success"))
        .when(requestConfigurationService)
        .doRequest(
            eq(appId),
            eq(terId),
            eq(type),
            eq(typeId),
            anyString(),
            any(),
            eq(requestUrl),
            isNull());

    // When: The controller processes the GET request
    ResponseEntity<?> result =
        proxyMiddlewareController.getService(
            appId, terId, type, typeId, token, queryParams, httpServletRequest);

    // Then: The service is called with correct parameters
    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

    verify(requestConfigurationService)
        .doRequest(
            eq(appId),
            eq(terId),
            eq(type),
            eq(typeId),
            tokenCaptor.capture(),
            paramsCaptor.capture(),
            urlCaptor.capture(),
            isNull());

    // Verify path parameters are correctly extracted
    assertThat(result.getStatusCode().value()).isEqualTo(200);

    // Verify token is correctly extracted (Bearer prefix removed)
    assertThat(tokenCaptor.getValue()).isEqualTo("test-jwt-token");

    // Verify query parameters are forwarded
    assertThat(paramsCaptor.getValue()).containsEntry("SERVICE", "WMS");
    assertThat(paramsCaptor.getValue()).containsEntry("LAYERS", "test:layer");

    // Verify URL is forwarded for backend validation
    assertThat(urlCaptor.getValue()).isEqualTo(requestUrl);
  }

  @Test
  @DisplayName("GET: URL without Authorization header is processed correctly")
  void getServiceWithoutAuthorizationHeader() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = "SQL";
    Integer typeId = 23;
    Map<String, String> queryParams = new HashMap<>();

    String requestUrl = "http://localhost:8080/proxy/1/2/SQL/23";
    when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(requestUrl));

    doReturn(ResponseEntity.ok("success"))
        .when(requestConfigurationService)
        .doRequest(
            eq(appId), eq(terId), eq(type), eq(typeId), isNull(), any(), eq(requestUrl), isNull());

    // When
    ResponseEntity<?> result =
        proxyMiddlewareController.getService(
            appId, terId, type, typeId, null, queryParams, httpServletRequest);

    // Then
    verify(requestConfigurationService)
        .doRequest(
            eq(appId), eq(terId), eq(type), eq(typeId), isNull(), any(), eq(requestUrl), isNull());
    assertThat(result.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName("POST: XML body is forwarded correctly with URL for validation")
  void postServiceWithXmlBodyForwardsToBackend() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = "WFS";
    Integer typeId = 50;
    String token = "Bearer test-jwt-token";
    Map<String, String> queryParams = new HashMap<>();
    String xmlBody =
        """
        <?xml version="1.0"?>
        <wfs:GetFeature service="WFS" version="2.0.0">
          <wfs:Query typeNames="test:layer"/>
        </wfs:GetFeature>
        """;

    String requestUrl = "http://localhost:8080/proxy/1/2/WFS/50";
    when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(requestUrl));

    doReturn(ResponseEntity.ok("wfs-response"))
        .when(requestConfigurationService)
        .doRequest(
            eq(appId),
            eq(terId),
            eq(type),
            eq(typeId),
            anyString(),
            any(),
            eq(requestUrl),
            eq(xmlBody));

    // When
    ResponseEntity<?> result =
        proxyMiddlewareController.postService(
            appId, terId, type, typeId, token, queryParams, httpServletRequest, xmlBody);

    // Then
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(requestConfigurationService)
        .doRequest(
            eq(appId),
            eq(terId),
            eq(type),
            eq(typeId),
            eq("test-jwt-token"),
            any(),
            eq(requestUrl),
            bodyCaptor.capture());

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(bodyCaptor.getValue()).contains("wfs:GetFeature");
  }

  @Test
  @DisplayName("GET: Empty query parameters map is handled correctly")
  void getServiceWithEmptyQueryParameters() {
    // Given
    Integer appId = 1;
    Integer terId = 0;
    String type = "WMTS";
    Integer typeId = 1;
    Map<String, String> emptyParams = new HashMap<>();

    String requestUrl = "http://localhost:8080/proxy/1/0/WMTS/1";
    when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(requestUrl));

    doReturn(ResponseEntity.ok("wmts-capabilities"))
        .when(requestConfigurationService)
        .doRequest(
            eq(appId), eq(terId), eq(type), eq(typeId), isNull(), any(), eq(requestUrl), isNull());

    // When
    ResponseEntity<?> result =
        proxyMiddlewareController.getService(
            appId, terId, type, typeId, null, emptyParams, httpServletRequest);

    // Then
    assertThat(result.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName("GET: Query parameters with special characters are forwarded correctly")
  void getServiceWithSpecialCharactersInQueryParameters() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = "WMS";
    Integer typeId = 100;
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("BBOX", "41.3,2.1,41.4,2.2");
    queryParams.put("CRS", "EPSG:4326");
    queryParams.put("FILTER", "name='Barcelona'");

    String requestUrl = "http://localhost:8080/proxy/1/2/WMS/100";
    when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(requestUrl));

    doReturn(ResponseEntity.ok("map-image"))
        .when(requestConfigurationService)
        .doRequest(
            eq(appId), eq(terId), eq(type), eq(typeId), isNull(), any(), eq(requestUrl), isNull());

    // When
    ResponseEntity<?> result =
        proxyMiddlewareController.getService(
            appId, terId, type, typeId, null, queryParams, httpServletRequest);

    // Then
    ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(requestConfigurationService)
        .doRequest(
            eq(appId),
            eq(terId),
            eq(type),
            eq(typeId),
            isNull(),
            paramsCaptor.capture(),
            eq(requestUrl),
            isNull());

    assertThat(paramsCaptor.getValue()).containsEntry("BBOX", "41.3,2.1,41.4,2.2");
    assertThat(paramsCaptor.getValue()).containsEntry("CRS", "EPSG:4326");
    assertThat(paramsCaptor.getValue()).containsEntry("FILTER", "name='Barcelona'");
    assertThat(result.getStatusCode().value()).isEqualTo(200);
  }
}
