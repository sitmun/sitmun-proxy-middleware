package org.sitmun.proxy.middleware.usage;

import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY;

import org.sitmun.proxy.contract.ServiceUsageReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProxyServiceUsagePoster implements ServiceUsagePoster {

  private final RestTemplate restTemplate;
  private final String reportUrl;
  private final String secret;

  public ProxyServiceUsagePoster(
      RestTemplate restTemplate,
      @Value("${sitmun.backend.config.url}") String configUrl,
      @Value("${sitmun.backend.config.secret}") String secret) {
    this.restTemplate = restTemplate;
    this.reportUrl = configUrl + "/service-usage";
    this.secret = secret;
  }

  @Override
  public void post(ServiceUsageReport report) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(PROXY_MIDDLEWARE_KEY, secret);
    headers.setContentType(MediaType.APPLICATION_JSON);
    try {
      restTemplate.postForEntity(reportUrl, new HttpEntity<>(report, headers), Void.class);
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
        throw new UsageEndpointMissing();
      }
      throw ex;
    }
  }
}
