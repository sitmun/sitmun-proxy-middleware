package org.sitmun.proxy.middleware.mbtiles;

import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.BackendConfigRequest;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.BackendConfigResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class MbtilesConfigClient {

  private final RestTemplate restTemplate;
  private final String configUrl;
  private final String secret;

  public MbtilesConfigClient(
      RestTemplate restTemplate,
      @Value("${sitmun.backend.config.url}") String configUrl,
      @Value("${sitmun.backend.config.secret}") String secret) {
    this.restTemplate = restTemplate;
    this.configUrl = trimTrailingSlash(configUrl) + "/mbtiles";
    this.secret = secret;
  }

  public BackendConfigResponse authorize(BackendConfigRequest body, String bearerToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add(PROXY_MIDDLEWARE_KEY, secret);
    headers.setBearerAuth(bearerToken);
    HttpEntity<BackendConfigRequest> entity = new HttpEntity<>(body, headers);
    try {
      ResponseEntity<BackendConfigResponse> response =
          restTemplate.exchange(configUrl, HttpMethod.POST, entity, BackendConfigResponse.class);
      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new MbtilesClientException(502, "Backend MBTiles configuration response was empty");
      }
      return response.getBody();
    } catch (HttpClientErrorException e) {
      log.warn("Backend MBTiles config rejected with status {}", e.getStatusCode().value());
      throw new MbtilesBackendException(e);
    } catch (MbtilesClientException e) {
      throw e;
    } catch (org.springframework.web.client.HttpServerErrorException e) {
      log.error(
          "Backend MBTiles config server error {}: {}",
          e.getStatusCode().value(),
          e.getResponseBodyAsString());
      throw new MbtilesClientException(502, "Backend configuration service is unavailable");
    } catch (Exception e) {
      log.error(
          "Backend MBTiles config failed with {}: {}",
          e.getClass().getSimpleName(),
          e.getMessage());
      throw new MbtilesClientException(502, "Backend configuration service is unavailable");
    }
  }

  private static String trimTrailingSlash(String url) {
    if (url != null && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }

  /** Carries the backend HTTP error for problem-detail mapping. */
  public static final class MbtilesBackendException extends RuntimeException {
    private final HttpClientErrorException cause;

    MbtilesBackendException(HttpClientErrorException cause) {
      super(cause);
      this.cause = cause;
    }

    public HttpClientErrorException clientError() {
      return cause;
    }
  }
}
