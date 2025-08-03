package org.sitmun.proxy.middleware.service;

import static org.sitmun.proxy.middleware.utils.LoggerUtils.logAsPrettyJson;

import java.util.Date;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.ConfigProxyRequestDto;
import org.sitmun.proxy.middleware.dto.ErrorResponseDto;
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
      String url) {
    ConfigProxyRequestDto configProxyRequest =
        new ConfigProxyRequestDto(appId, terId, type, typeId, "GET", params, null, token);
    logAsPrettyJson(log, "Request to the API:\n{}", configProxyRequest);

    ResponseEntity<?> response = configRequest(configProxyRequest);
    if (response.getStatusCode().value() == 200) {

      ConfigProxyDto configProxyDto = (ConfigProxyDto) response.getBody();
      logAsPrettyJson(log, "Response from the API:\n{}", configProxyDto);

      if (configProxyDto != null) {
        log.info("Requesting data from the final service");
        return requestExecutorService.executeRequest(url, configProxyDto.getPayload());
      } else {
        ErrorResponseDto errorResponse =
            new ErrorResponseDto(401, "Bad Request", "Request not valid", configUrl, new Date());
        return ResponseEntity.status(401).body(errorResponse);
      }
    } else {
      return response;
    }
  }

  private ResponseEntity<?> configRequest(ConfigProxyRequestDto configRequest) {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("X-SITMUN-Proxy-Key", this.secret);
    HttpEntity<ConfigProxyRequestDto> httpEntity = new HttpEntity<>(configRequest, requestHeaders);
    try {
      return restTemplate.exchange(configUrl, HttpMethod.POST, httpEntity, ConfigProxyDto.class);
    } catch (HttpClientErrorException e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      ErrorResponseDto errorResponse =
          new ErrorResponseDto(
              e.getStatusCode().value(), "", e.getMessage(), configUrl, new Date());
      return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    } catch (Exception e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      ErrorResponseDto errorResponse =
          new ErrorResponseDto(500, "", e.getMessage(), configUrl, new Date());
      return ResponseEntity.status(500).body(errorResponse);
    }
  }
}
