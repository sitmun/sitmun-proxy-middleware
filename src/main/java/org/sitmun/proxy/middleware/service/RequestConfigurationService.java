package org.sitmun.proxy.middleware.service;

import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.backendAuthorizationFailure;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.backendFailure;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.backendUnavailable;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.emptyBackendConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.ConfigProxyRequestDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;
import org.sitmun.proxy.middleware.utils.logging.SensitiveDataMasking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RequestConfigurationService {

  private final RestTemplate restTemplate;
  private final RequestExecutorService requestExecutorService;
  private final ObjectMapper objectMapper;

  @Value("${sitmun.backend.config.url}")
  private String configUrl;

  @Value("${sitmun.backend.config.secret}")
  private String secret;

  public RequestConfigurationService(
      RestTemplate restTemplate,
      RequestExecutorService requestExecutorService,
      ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.requestExecutorService = requestExecutorService;
    this.objectMapper = objectMapper;
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
        new ConfigProxyRequestDto(appId, terId, type, typeId, method, params, body);
    log.debug(
        "Proxy config request: appId={} terId={} type={} typeId={} method={} paramKeys={} "
            + "upstreamRequestBodyPresent={} authorizationPresent={}",
        appId,
        terId,
        type,
        typeId,
        method,
        params == null ? 0 : params.size(),
        body != null,
        StringUtils.hasText(token));

    ResponseEntity<?> response = configRequest(configProxyRequest, token);
    if (response.getStatusCode().value() == 200) {

      ConfigProxyDto configProxyDto = (ConfigProxyDto) response.getBody();
      log.debug(
          "Proxy config response: status=200 configType={} exp={} payloadKind={} detail={}",
          configProxyDto != null ? configProxyDto.getType() : null,
          configProxyDto != null ? configProxyDto.getExp() : 0,
          configProxyDto != null && configProxyDto.getPayload() != null
              ? configProxyDto.getPayload().getClass().getSimpleName()
              : "null",
          describePayloadForLog(configProxyDto != null ? configProxyDto.getPayload() : null));

      if (configProxyDto != null) {
        log.info("Requesting data from the final service");
        return requestExecutorService.executeRequest(url, configProxyDto.getPayload());
      } else {
        return emptyBackendConfiguration();
      }
    } else {
      return response;
    }
  }

  private ResponseEntity<?> configRequest(ConfigProxyRequestDto configRequest, String token) {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add(PROXY_MIDDLEWARE_KEY, this.secret);
    if (StringUtils.hasText(token)) {
      requestHeaders.setBearerAuth(token);
    }
    log.debug(
        "Calling backend config: outboundHeaders={} authorizationPresent={} requestBodyPresent={}",
        SensitiveDataMasking.formatMaskedMap(Map.of(PROXY_MIDDLEWARE_KEY, secret)),
        StringUtils.hasText(token),
        StringUtils.hasText(configRequest.getRequestBody()));
    HttpEntity<ConfigProxyRequestDto> httpEntity = new HttpEntity<>(configRequest, requestHeaders);
    try {
      return restTemplate.exchange(configUrl, HttpMethod.POST, httpEntity, ConfigProxyDto.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
        log.warn(
            "Backend configuration request rejected with status {}", e.getStatusCode().value());
        return backendAuthorizationFailure(e, objectMapper);
      }
      log.error("Backend configuration request failed with status {}", e.getStatusCode().value());
      return backendFailure(HttpStatus.valueOf(e.getStatusCode().value()));
    } catch (Exception e) {
      log.error(
          "Backend configuration request failed with exception type {}",
          e.getClass().getSimpleName());
      return backendUnavailable();
    }
  }

  private static String describePayloadForLog(PayloadDto payload) {
    if (payload == null) {
      return "null";
    }
    if (payload instanceof WmsPayloadDto wms) {
      var sec = wms.getSecurity();
      return "method="
          + wms.getMethod()
          + ", "
          + (sec == null ? "security=null" : sec.describeForLog());
    }
    if (payload instanceof JdbcPayloadDto) {
      return "jdbc payload (credentials omitted)";
    }
    return payload.getClass().getSimpleName();
  }
}
