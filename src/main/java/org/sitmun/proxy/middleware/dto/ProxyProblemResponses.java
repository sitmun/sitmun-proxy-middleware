package org.sitmun.proxy.middleware.dto;

import static org.sitmun.proxy.middleware.dto.ProblemTypes.*;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public final class ProxyProblemResponses {

  private static final String SITMUN_PROBLEM_PREFIX = "https://sitmun.org/problems/";
  private static final String PROXY_INSTANCE = "/proxy";

  private ProxyProblemResponses() {}

  public static ResponseEntity<ProblemDetail> invalidAuthorizationHeader() {
    return response(
        HttpStatus.BAD_REQUEST,
        ProblemDetail.builder()
            .type(PROXY_INVALID_REQUEST)
            .status(HttpStatus.BAD_REQUEST.value())
            .title("Invalid Authorization Header")
            .detail("Authorization header must contain one Bearer token")
            .instance(PROXY_INSTANCE)
            .properties(Map.of("origin", "proxy-request"))
            .build());
  }

  public static ResponseEntity<ProblemDetail> emptyBackendConfiguration() {
    return response(
        HttpStatus.BAD_GATEWAY,
        ProblemDetail.builder()
            .type(PROXY_CONFIG_ERROR)
            .status(HttpStatus.BAD_GATEWAY.value())
            .title("Proxy Configuration Error")
            .detail("Backend configuration response was empty")
            .instance(PROXY_INSTANCE)
            .properties(Map.of("origin", "backend-config"))
            .build());
  }

  public static ResponseEntity<ProblemDetail> backendAuthorizationFailure(
      HttpClientErrorException exception, ObjectMapper objectMapper) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    JsonNode backendProblem = parseProblem(exception.getResponseBodyAsString(), objectMapper);
    String fallbackType = status == HttpStatus.UNAUTHORIZED ? PROXY_UNAUTHORIZED : PROXY_FORBIDDEN;
    String fallbackTitle = status == HttpStatus.UNAUTHORIZED ? "Unauthorized" : "Forbidden";
    Optional<String> backendType = safeProblemType(backendProblem);

    ProblemDetail problem =
        ProblemDetail.builder()
            .type(backendType.orElse(fallbackType))
            .status(status.value())
            .title(backendType.flatMap(ignored -> safeTitle(backendProblem)).orElse(fallbackTitle))
            .detail("Backend configuration request was rejected")
            .instance(PROXY_INSTANCE)
            .properties(Map.of("origin", "backend-config"))
            .build();
    return response(status, problem);
  }

  public static ResponseEntity<ProblemDetail> backendFailure(HttpStatus status) {
    return response(
        status,
        ProblemDetail.builder()
            .type(PROXY_BACKEND_ERROR)
            .status(status.value())
            .title("Backend Error")
            .detail("Backend configuration request failed")
            .instance(PROXY_INSTANCE)
            .properties(Map.of("origin", "backend-config"))
            .build());
  }

  public static ResponseEntity<ProblemDetail> backendUnavailable() {
    return response(
        HttpStatus.BAD_GATEWAY,
        ProblemDetail.builder()
            .type(PROXY_CONFIG_ERROR)
            .status(HttpStatus.BAD_GATEWAY.value())
            .title("Proxy Configuration Error")
            .detail("Backend configuration service is unavailable")
            .instance(PROXY_INSTANCE)
            .properties(Map.of("origin", "backend-config"))
            .build());
  }

  public static ProblemDetail upstreamAuthorizationFailure() {
    return ProblemDetail.builder()
        .type(PROXY_UPSTREAM_AUTHORIZATION_ERROR)
        .status(HttpStatus.BAD_GATEWAY.value())
        .title("Upstream Authorization Error")
        .detail("The upstream service rejected proxy authorization")
        .instance(PROXY_INSTANCE)
        .properties(Map.of("origin", "upstream-service"))
        .build();
  }

  private static ResponseEntity<ProblemDetail> response(
      HttpStatus status, ProblemDetail problemDetail) {
    return ResponseEntity.status(status).contentType(APPLICATION_PROBLEM_JSON).body(problemDetail);
  }

  private static JsonNode parseProblem(String body, ObjectMapper objectMapper) {
    try {
      return objectMapper.readTree(body);
    } catch (Exception ignored) {
      return objectMapper.nullNode();
    }
  }

  private static Optional<String> safeProblemType(JsonNode problem) {
    return text(problem, "type").filter(value -> value.startsWith(SITMUN_PROBLEM_PREFIX));
  }

  private static Optional<String> safeTitle(JsonNode problem) {
    return text(problem, "title")
        .filter(value -> value.length() <= 100)
        .filter(value -> value.chars().noneMatch(Character::isISOControl));
  }

  private static Optional<String> text(JsonNode problem, String field) {
    if (problem == null || !problem.isObject()) {
      return Optional.empty();
    }
    JsonNode value = problem.get(field);
    return value != null && value.isTextual() && !value.textValue().isBlank()
        ? Optional.of(value.textValue())
        : Optional.empty();
  }
}
