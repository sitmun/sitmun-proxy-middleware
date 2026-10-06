package org.sitmun.proxy.middleware.servicecheck;

import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY;

import org.sitmun.proxy.contract.ServiceCheckReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProxyServiceCheckPoster implements ServiceCheckPoster {

  private final RestTemplate restTemplate;
  private final String reportUrl;
  private final String secret;

  public ProxyServiceCheckPoster(
      RestTemplate restTemplate,
      @Value("${sitmun.backend.config.url}") String configUrl,
      @Value("${sitmun.backend.config.secret}") String secret) {
    this.restTemplate = restTemplate;
    this.reportUrl = configUrl + "/service-checks";
    this.secret = secret;
  }

  @Override
  public void post(ServiceCheckReport report) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(PROXY_MIDDLEWARE_KEY, secret);
    headers.setContentType(MediaType.APPLICATION_JSON);
    restTemplate.postForEntity(reportUrl, new HttpEntity<>(report, headers), Void.class);
  }
}
