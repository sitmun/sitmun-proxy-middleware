package org.sitmun.proxy.middleware.protocols.http;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.sitmun.proxy.middleware.dto.ErrorResponseDto;
import org.sitmun.proxy.middleware.service.RequestExecutor;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;
import org.sitmun.proxy.middleware.service.RequestExecutorResponseImpl;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.*;

@Slf4j
public class HttpRequestExecutor implements RequestExecutor {

  private final Map<String, String> headers = new HashMap<>();
  private final Map<String, String> parameters = new HashMap<>();
  private final HttpClient httpClient;
  private final String baseUrl;
  @Setter private String url;
  @Setter private String body;

  public HttpRequestExecutor(String baseUrl, HttpClient httpClient) {
    this.baseUrl = baseUrl;
    this.httpClient = httpClient;
  }

  public void setHeader(String header, String value) {
    headers.put(header, value);
  }

  public void setParameters(Map<String, String> parameters) {
    if (parameters != null && !parameters.isEmpty()) {
      this.parameters.putAll(parameters);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public RequestExecutorResponse<?> execute() {
    if (!StringUtils.hasText(url)) {
      throw new IllegalStateException("Url is not set");
    }

    okhttp3.Request.Builder builder = new okhttp3.Request.Builder();

    builder.url(getUrl());
    for (String k : headers.keySet()) {
      builder.addHeader(k, headers.get(k));
    }

    if (body != null) {
      RequestBody requestBody =
          RequestBody.create((String) body, okhttp3.MediaType.parse("text/xml"));
      builder.post(requestBody);
    }

    okhttp3.Request httpRequest = builder.build();

    log.info("Executing request to: {}", httpRequest.url());
    log.info("Method: {}", httpRequest.method());
    log.info("Headers: {}", httpRequest.headers());
    log.info("Base URL: {}", baseUrl);

    try (okhttp3.Response r = httpClient.executeRequest(httpRequest)) {
      ResponseBody body = r.body();
      if (body == null)
        return new RequestExecutorResponseImpl<>(baseUrl, r.code(), r.header("content-type"), null);
      return new RequestExecutorResponseImpl<>(
          baseUrl, r.code(), r.header("content-type"), body.bytes());
    } catch (IOException e) {
      log.error("Error getting response: {}", e.getMessage(), e);
      return new RequestExecutorResponseImpl<>(
          baseUrl,
          500,
          "application/json",
          new ErrorResponseDto(
              500, "ServiceError", "Error with the request to final service", "", new Date()));
    }
  }

  public String getUrl() {
    if (!parameters.isEmpty()) {
      UriComponents components = UriComponentsBuilder.fromUriString(url).build();

      MultiValueMap<String, String> queryParams =
          new LinkedMultiValueMap<>(components.getQueryParams().size());

      components.getQueryParams().forEach((k, v) -> queryParams.put(k.toUpperCase(), v));
      parameters.forEach(
          (k, v) -> {
            String upperKey = k.toUpperCase();
            List<String> existingValues = queryParams.get(upperKey);
            if (existingValues == null || !existingValues.contains(v)) {
              queryParams.add(upperKey, v);
            }
          });

      String path = components.getPath() != null ? components.getPath() : "";

      UriComponentsBuilder builder =
          UriComponentsBuilder.newInstance()
              .scheme(components.getScheme())
              .host(components.getHost())
              .port(components.getPort())
              .path(path)
              .queryParams(queryParams);

      log.info("path: {}", components.getPath());
      log.info("query: {}", queryParams);

      return builder.toUriString();
    } else {
      return url;
    }
  }

  public String describe() {
    return "HttpRequest{"
        + "url='"
        + url
        + '\''
        + ", headers="
        + headers
        + ", parameters="
        + parameters
        + ", baseUrl="
        + baseUrl
        + '}';
  }
}
