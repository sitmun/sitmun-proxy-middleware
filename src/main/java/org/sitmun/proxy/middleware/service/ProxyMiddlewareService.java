package org.sitmun.proxy.middleware.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.ConfigProxyRequest;
import org.sitmun.proxy.middleware.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Map;

import static org.sitmun.proxy.middleware.utils.LoggerUtils.logAsPrettyJson;

@Service
@Slf4j
public class ProxyMiddlewareService {

  @Value("${sitmun.config.url}")
  private String configUrl;

  private final RestTemplate restTemplate;

  private final GlobalRequestService globalRequestService;

  @Value("${security.authentication.middleware.secret}")
  private String secret;

  public ProxyMiddlewareService(RestTemplate restTemplate, GlobalRequestService globalRequestService) {
    this.restTemplate = restTemplate;
    this.globalRequestService = globalRequestService;
  }

  public ResponseEntity<?> doRequest(Integer appId, Integer terId, String type,
                                     Integer typeId, String token, Map<String, String> params) {
    ConfigProxyRequest configProxyRequest = new ConfigProxyRequest(appId, terId, type, typeId, "GET", params, null, token);
    logAsPrettyJson(log, "Request to the API:\n{}", configProxyRequest);

    ResponseEntity<?> response = configRequest(configProxyRequest);
    if (response.getStatusCodeValue() == 200) {

      ConfigProxyDto configProxyDto = (ConfigProxyDto) response.getBody();
      logAsPrettyJson(log, "Response from the API:\n{}", configProxyDto);

      if (configProxyDto != null) {
        return globalRequestService.executeRequest(configProxyDto.getPayload());
      } else {
        return null;
      }
    } else {
      return response;
    }
  }

  private ResponseEntity<?> configRequest(ConfigProxyRequest configRequest) {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("X-SITMUN-Proxy-Key", this.secret);
    HttpEntity<ConfigProxyRequest> httpEntity = new HttpEntity<>(configRequest, requestHeaders);
    ResponseEntity<?> serviceResponse = null;
    try {
      serviceResponse = restTemplate.exchange(configUrl, HttpMethod.POST, httpEntity, ConfigProxyDto.class);
    } catch (HttpClientErrorException e) {
      ErrorResponseDTO errorResponse = new ErrorResponseDTO(e.getRawStatusCode(), "", e.getMessage(), configUrl, new Date());

      serviceResponse = ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }
    return serviceResponse;
  }
}
