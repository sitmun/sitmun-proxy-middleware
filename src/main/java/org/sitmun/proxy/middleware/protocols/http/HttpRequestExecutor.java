package org.sitmun.proxy.middleware.protocols.http;

import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.upstreamAuthorizationFailure;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.sitmun.proxy.middleware.service.RequestExecutor;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;
import org.sitmun.proxy.middleware.service.RequestExecutorResponseImpl;
import org.sitmun.proxy.middleware.utils.UriTemplateExpander;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.*;

@Slf4j
public class HttpRequestExecutor implements RequestExecutor {

  private static final String DEFAULT_POST_CONTENT_TYPE = "text/xml";
  private static final Set<String> HOP_BY_HOP_HEADERS =
      Set.of(
          "connection",
          "keep-alive",
          "proxy-authenticate",
          "proxy-authorization",
          "te",
          "trailers",
          "transfer-encoding",
          "upgrade",
          "authorization",
          "cookie",
          "set-cookie",
          "www-authenticate");

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

  /** Returns the header value set on this executor (for tests). */
  public String getHeader(String name) {
    return headers.get(name);
  }

  public void setParameters(Map<String, String> parameters) {
    if (parameters != null && !parameters.isEmpty()) {
      this.parameters.putAll(parameters);
    }
  }

  public void addParameter(String key, String value) {
    if (StringUtils.hasText(key) && value != null) {
      this.parameters.put(key, value);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public RequestExecutorResponse<?> execute() {
    if (!StringUtils.hasText(url)) {
      throw new IllegalStateException("Url is not set");
    }

    okhttp3.Request httpRequest = buildRequest();

    log.debug(
        "Executing upstream request: method={} headerNames={}",
        httpRequest.method(),
        httpRequest.headers().names());

    try (okhttp3.Response r = httpClient.executeRequest(httpRequest)) {
      if (r.code() == 401 || r.code() == 403) {
        return new RequestExecutorResponseImpl<>(
            baseUrl, 502, "application/problem+json", upstreamAuthorizationFailure());
      }
      ResponseBody body = r.body();
      if (body == null)
        return new RequestExecutorResponseImpl<>(baseUrl, r.code(), r.header("content-type"), null);
      return new RequestExecutorResponseImpl<>(
          baseUrl, r.code(), r.header("content-type"), body.bytes());
    } catch (IOException e) {
      log.error("Upstream request failed with exception type {}", e.getClass().getSimpleName());
      org.sitmun.proxy.middleware.dto.ProblemDetail problem =
          org.sitmun.proxy.middleware.dto.ProblemDetail.builder()
              .type(org.sitmun.proxy.middleware.dto.ProblemTypes.PROXY_SERVICE_ERROR)
              .status(503)
              .title("Service Error")
              .detail("Error with the request to final service")
              .instance("")
              .build();
      return new RequestExecutorResponseImpl<>(baseUrl, 503, "application/problem+json", problem);
    }
  }

  /**
   * Executes the request and returns a streamable body without buffering via {@code body.bytes()}.
   * Caller must close the returned response.
   */
  public StreamedHttpResponse executeStreaming() throws IOException {
    if (!StringUtils.hasText(url)) {
      throw new IllegalStateException("Url is not set");
    }

    okhttp3.Request httpRequest = buildRequest();
    log.debug(
        "Executing streaming upstream request: method={} headerNames={}",
        httpRequest.method(),
        httpRequest.headers().names());

    okhttp3.Response response = httpClient.executeRequest(httpRequest);
    ResponseBody responseBody = response.body();
    InputStream stream =
        responseBody != null ? responseBody.byteStream() : InputStream.nullInputStream();
    return new StreamedHttpResponse(response, stream);
  }

  private okhttp3.Request buildRequest() {
    okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
    builder.url(getUrl());
    for (String k : headers.keySet()) {
      builder.addHeader(k, headers.get(k));
    }

    if (body != null) {
      String contentType = resolvePostContentType();
      RequestBody requestBody = RequestBody.create(body, okhttp3.MediaType.parse(contentType));
      builder.post(requestBody);
    }
    return builder.build();
  }

  private String resolvePostContentType() {
    String fromHeader = headers.get("Content-Type");
    if (!StringUtils.hasText(fromHeader)) {
      fromHeader = headers.get("content-type");
    }
    return StringUtils.hasText(fromHeader) ? fromHeader : DEFAULT_POST_CONTENT_TYPE;
  }

  public String getUrl() {
    if (parameters.isEmpty()) {
      return url;
    }

    // Check if URL contains URI template variables
    if (UriTemplateExpander.hasTemplateVariables(url)) {
      // Expand template variables and get which ones were used
      UriTemplateExpander.ExpandedResult result =
          UriTemplateExpander.expandWithUsedVariables(url, parameters);
      String expandedUrl = result.getUri();

      // If all parameters were used in template expansion, return the expanded URL
      if (result.getUsedVariables().size() == parameters.size()) {
        return expandedUrl;
      }

      // Some parameters weren't used in template, add them as query parameters
      UriComponents components = UriComponentsBuilder.fromUriString(expandedUrl).build();
      return rebuildUrlWithMergedQueryParams(
          components, k -> !result.getUsedVariables().contains(k));
    }

    // No template variables, just add parameters as query strings
    UriComponents components = UriComponentsBuilder.fromUriString(url).build();
    return rebuildUrlWithMergedQueryParams(components, k -> true);
  }

  private String rebuildUrlWithMergedQueryParams(
      UriComponents components, Predicate<String> includeParameterKey) {
    MultiValueMap<String, String> queryParams =
        new LinkedMultiValueMap<>(components.getQueryParams().size());

    queryParams.putAll(components.getQueryParams());

    parameters.forEach(
        (k, v) -> {
          if (!includeParameterKey.test(k)) {
            return;
          }
          List<String> existingValues = queryParams.get(k);
          if (existingValues == null || !existingValues.contains(v)) {
            queryParams.add(k, v);
          }
        });

    String path = components.getPath() != null ? components.getPath() : "";

    log.debug("Built upstream request with {} query parameter names", queryParams.size());

    return UriComponentsBuilder.newInstance()
        .scheme(components.getScheme())
        .host(components.getHost())
        .port(components.getPort())
        .path(path)
        .queryParams(queryParams)
        .toUriString();
  }

  public String describe() {
    return "HttpRequest{"
        + "method="
        + (body == null ? "GET" : "POST")
        + ", headerNames="
        + headers.keySet()
        + ", parameterNames="
        + parameters.keySet()
        + '}';
  }

  /** Whether an upstream response header should be forwarded to the client. */
  public static boolean isForwardableResponseHeader(String name) {
    return name != null && !HOP_BY_HOP_HEADERS.contains(name.toLowerCase());
  }

  /** Open OkHttp response with a body stream; must be closed by the caller. */
  public record StreamedHttpResponse(okhttp3.Response response, InputStream bodyStream)
      implements AutoCloseable {
    @Override
    public void close() {
      response.close();
    }
  }
}
