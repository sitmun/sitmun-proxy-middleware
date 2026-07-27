package org.sitmun.proxy.middleware.mbtiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sitmun.proxy.middleware.protocols.http.HttpRequestExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Calls the configured MBTiles service. Never forwards client Authorization or proxy key headers.
 * Relative paths are restricted to {@code ""}, {@code estimate}, or decoded internal job id paths.
 */
@Service
@Slf4j
public class MbtilesUpstreamClient {

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient;
  private final String baseUrl;
  private final ObjectMapper objectMapper;

  public MbtilesUpstreamClient(MbtilesProperties properties, ObjectMapper objectMapper) {
    this.baseUrl = trimTrailingSlash(properties.url());
    this.objectMapper = objectMapper;
    Duration connect = properties.connectTimeout();
    Duration read = properties.readTimeout();
    this.httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(connect.toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(read.toMillis(), TimeUnit.MILLISECONDS)
            .build();
  }

  public String postEstimate(Object tileRequest) throws IOException {
    return executeText(post(relativeUrl("estimate"), tileRequest));
  }

  public String postCreate(Object tileRequest) throws IOException {
    return executeText(post(relativeUrl(""), tileRequest)).trim();
  }

  public String getStatus(long jobId) throws IOException {
    return executeText(get(jobPath(jobId)));
  }

  public HttpRequestExecutor.StreamedHttpResponse getFile(long jobId) throws IOException {
    Request request = new Request.Builder().url(jobPath(jobId) + "/file").get().build();
    Response response = httpClient.newCall(request).execute();
    return new HttpRequestExecutor.StreamedHttpResponse(
        response,
        response.body() != null
            ? response.body().byteStream()
            : java.io.InputStream.nullInputStream());
  }

  private Request post(String url, Object tileRequest) throws IOException {
    String json = objectMapper.writeValueAsString(tileRequest);
    return new Request.Builder()
        .url(url)
        .post(RequestBody.create(json, JSON))
        .header("Content-Type", "application/json")
        .build();
  }

  private Request get(String url) {
    return new Request.Builder().url(url).get().build();
  }

  private String executeText(Request request) throws IOException {
    try (Response response = httpClient.newCall(request).execute()) {
      if (response.code() == 401 || response.code() == 403) {
        throw new MbtilesClientException(502, "The upstream service rejected proxy authorization");
      }
      String body = response.body() != null ? response.body().string() : "";
      if (!response.isSuccessful()) {
        throw new MbtilesClientException(
            response.code() >= 500 ? 502 : 400, "MBTiles upstream request failed");
      }
      return body;
    }
  }

  private String relativeUrl(String relative) {
    if (!StringUtils.hasText(relative)) {
      return baseUrl;
    }
    if (!"estimate".equals(relative)) {
      throw new IllegalArgumentException("Unsupported MBTiles relative path");
    }
    return baseUrl + "/" + relative;
  }

  private String jobPath(long jobId) {
    if (jobId <= 0) {
      throw new IllegalArgumentException("Invalid job id");
    }
    return baseUrl + "/" + jobId;
  }

  private static String trimTrailingSlash(String url) {
    if (url != null && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }
}
