package org.sitmun.proxy.middleware.request;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.sitmun.proxy.middleware.decorator.DecoratedRequest;
import org.sitmun.proxy.middleware.decorator.DecoratedResponse;
import org.sitmun.proxy.middleware.dto.ErrorResponseDTO;
import org.sitmun.proxy.middleware.response.Response;
import org.sitmun.proxy.middleware.service.ClientService;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpRequest implements DecoratedRequest {

  private final Map<String, String> headers = new HashMap<>();
  private final Map<String, String> parameters = new HashMap<>();
  private final ClientService clientService;
  @Setter
  private String url;

  public HttpRequest(ClientService clientService) {
    this.clientService = clientService;
  }

  public void setHeader(String header, String value) {
    headers.put(header, value);
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters.putAll(parameters);
  }

  @Override
  public DecoratedResponse<?> execute() {
    if (!StringUtils.hasText(url)) {
      throw new IllegalStateException("Url is not set");
    }

    okhttp3.Request.Builder builder = new okhttp3.Request.Builder();

    builder.url(getUrl());
    headers.keySet().forEach(k -> builder.addHeader(k, headers.get(k)));

    okhttp3.Request httpRequest = builder.build();

    log.info("Executing request to: {}", httpRequest.url());
    log.info("Method: {}", httpRequest.method());
    log.info("Headers: {}", httpRequest.headers());

    try (okhttp3.Response r = clientService.executeRequest(httpRequest)) {
      ResponseBody body = r.body();
      if (body == null) return new Response<>(r.code(), r.header("content-type"), null);
      return new Response<>(r.code(), r.header("content-type"), body.bytes());
    } catch (IOException e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      return new Response<>(500, "application/json", new ErrorResponseDTO(500, "ServiceError", "Error with the request to final service", "", new Date()));
    }
  }

  public String getUrl() {
    if (!parameters.isEmpty()) {
      StringBuilder uri = new StringBuilder(url).append('?');
      parameters.keySet().forEach(k -> uri.append(k).append("=").append(parameters.get(k)).append("&"));
      uri.deleteCharAt(uri.length() - 1);
      return uri.toString();
    } else {
      return url;
    }
  }

  public String describe() {
    return "HttpRequest{" +
      "url='" + url + '\'' +
      ", headers=" + headers +
      ", parameters=" + parameters +
      '}';
  }
}
