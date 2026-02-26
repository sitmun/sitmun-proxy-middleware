package org.sitmun.proxy.middleware.service;

import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY;
import static org.sitmun.proxy.middleware.dto.ProblemTypes.*;
import static org.sitmun.proxy.middleware.utils.LoggerUtils.logAsPrettyJson;
import static org.springframework.http.MediaType.*;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.ConfigProxyRequestDto;
import org.sitmun.proxy.middleware.dto.ProblemDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RequestConfigurationService {

  private final RestTemplate restTemplate;
  private final RequestExecutorService requestExecutorService;

  @Value("${sitmun.backend.config.url}")
  private String configUrl;

  @Value("${sitmun.backend.config.secret}")
  private String secret;

  public RequestConfigurationService(
      RestTemplate restTemplate, RequestExecutorService requestExecutorService) {
    this.restTemplate = restTemplate;
    this.requestExecutorService = requestExecutorService;
  }

  public ResponseEntity<?> doRequest(
      Integer appId,
      Integer terId,
      String type,
      Integer typeId,
      String token,
      Map<String, String> params,
      String url,
      String body) {
    String method = body == null ? "GET" : "POST";
    ConfigProxyRequestDto configProxyRequest =
        new ConfigProxyRequestDto(appId, terId, type, typeId, method, params, body, token);
    logAsPrettyJson(log, "Request to the API:\n{}", configProxyRequest);

    ResponseEntity<?> response = configRequest(configProxyRequest);
    if (response.getStatusCode().value() == 200) {

      ConfigProxyDto configProxyDto = (ConfigProxyDto) response.getBody();
      logAsPrettyJson(log, "Response from the API:\n{}", configProxyDto);

      if (configProxyDto != null) {
        log.info("Requesting data from the final service");
        return requestExecutorService.executeRequest(url, configProxyDto.getPayload());
      } else {
        ProblemDetail problem =
            ProblemDetail.builder()
                .type(PROXY_UNAUTHORIZED)
                .status(401)
                .title("Unauthorized")
                .detail("Request not valid")
                .instance(configUrl)
                .build();
        return ResponseEntity.status(401).contentType(APPLICATION_PROBLEM_JSON).body(problem);
      }
    } else {
      return response;
    }
  }

  private ResponseEntity<?> configRequest(ConfigProxyRequestDto configRequest) {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add(PROXY_MIDDLEWARE_KEY, this.secret);
    HttpEntity<ConfigProxyRequestDto> httpEntity = new HttpEntity<>(configRequest, requestHeaders);
    try {
      return restTemplate.exchange(configUrl, HttpMethod.POST, httpEntity, ConfigProxyDto.class);
    } catch (HttpClientErrorException e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      ProblemDetail problem =
          ProblemDetail.builder()
              .type(PROXY_BACKEND_ERROR)
              .status(e.getStatusCode().value())
              .title("Backend Error")
              .detail(e.getMessage())
              .instance(configUrl)
              .build();
      return ResponseEntity.status(e.getStatusCode())
          .contentType(APPLICATION_PROBLEM_JSON)
          .body(problem);
    } catch (Exception e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      ProblemDetail problem =
          ProblemDetail.builder()
              .type(PROXY_CONFIG_ERROR)
              .status(500)
              .title("Proxy Configuration Error")
              .detail(e.getMessage())
              .instance(configUrl)
              .build();
      return ResponseEntity.status(500).contentType(APPLICATION_PROBLEM_JSON).body(problem);
    }
  }
}
