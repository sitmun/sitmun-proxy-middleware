package org.sitmun.proxy.middleware.mbtiles;

import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.backendAuthorizationFailure;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.backendUnavailable;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.invalidAuthorizationHeader;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.missingAuthorizationHeader;
import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.proxyInvalidRequest;
import static org.sitmun.proxy.middleware.protocols.http.HttpRequestExecutor.isForwardableResponseHeader;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.controllers.AuthorizationBearerParser;
import org.sitmun.proxy.middleware.controllers.AuthorizationBearerParser.AuthorizationToken;
import org.sitmun.proxy.middleware.mbtiles.MbtilesConfigClient.MbtilesBackendException;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.BackendConfigRequest;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.BackendConfigResponse;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.ClientCreateRequest;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.CreateResponse;
import org.sitmun.proxy.middleware.mbtiles.MbtilesJobHandleService.JobHandleClaims;
import org.sitmun.proxy.middleware.protocols.http.HttpRequestExecutor.StreamedHttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
@Slf4j
public class MbtilesProxyService {

  private final MbtilesRequestValidator validator;
  private final MbtilesConfigClient configClient;
  private final MbtilesUpstreamClient upstreamClient;
  private final MbtilesJobHandleService jobHandleService;
  private final JwtPrincipalExtractor principalExtractor;
  private final ObjectMapper objectMapper;

  public MbtilesProxyService(
      MbtilesRequestValidator validator,
      MbtilesConfigClient configClient,
      MbtilesUpstreamClient upstreamClient,
      MbtilesJobHandleService jobHandleService,
      JwtPrincipalExtractor principalExtractor,
      ObjectMapper objectMapper) {
    this.validator = validator;
    this.configClient = configClient;
    this.upstreamClient = upstreamClient;
    this.jobHandleService = jobHandleService;
    this.principalExtractor = principalExtractor;
    this.objectMapper = objectMapper;
  }

  public ResponseEntity<?> estimate(
      int appId, int terId, String authorization, String rawBody, ClientCreateRequest body) {
    return withMandatoryBearer(
        authorization,
        token -> {
          validator.validateJsonSize(rawBody);
          validator.validateCreateOrEstimate(body);
          BackendConfigResponse config =
              authorize(
                  new BackendConfigRequest(
                      appId,
                      terId,
                      "estimate",
                      null,
                      body.bbox(),
                      body.minZoom(),
                      body.maxZoom(),
                      body.srs(),
                      body.services()),
                  token);
          try {
            String estimateJson = upstreamClient.postEstimate(config.tileRequest());
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.readTree(estimateJson));
          } catch (IOException e) {
            log.error("MBTiles estimate failed with {}", e.getClass().getSimpleName());
            return upstreamUnavailable();
          }
        });
  }

  public ResponseEntity<?> create(
      int appId, int terId, String authorization, String rawBody, ClientCreateRequest body) {
    return withMandatoryBearer(
        authorization,
        token -> {
          validator.validateJsonSize(rawBody);
          validator.validateCreateOrEstimate(body);
          BackendConfigResponse config =
              authorize(
                  new BackendConfigRequest(
                      appId,
                      terId,
                      "create",
                      null,
                      body.bbox(),
                      body.minZoom(),
                      body.maxZoom(),
                      body.srs(),
                      body.services()),
                  token);
          try {
            String jobIdText = upstreamClient.postCreate(config.tileRequest());
            long jobId = Long.parseLong(jobIdText.trim());
            String principal =
                principalExtractor
                    .extractSub(token)
                    .filter(StringUtils::hasText)
                    .orElse(config.username());
            String handle = jobHandleService.mint(principal, appId, terId, jobId);
            return ResponseEntity.ok(new CreateResponse(handle));
          } catch (NumberFormatException e) {
            log.error("MBTiles create returned non-numeric job id");
            return upstreamUnavailable();
          } catch (IOException e) {
            log.error("MBTiles create failed with {}", e.getClass().getSimpleName());
            return upstreamUnavailable();
          }
        });
  }

  public ResponseEntity<?> status(int appId, int terId, String authorization, String jobHandle) {
    return withMandatoryBearer(
        authorization,
        token -> {
          long jobId = resolveJobId(appId, terId, token, jobHandle, "status");
          try {
            String statusJson = upstreamClient.getStatus(jobId);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.readTree(statusJson));
          } catch (IOException e) {
            log.error("MBTiles status failed with {}", e.getClass().getSimpleName());
            return upstreamUnavailable();
          }
        });
  }

  public ResponseEntity<?> file(int appId, int terId, String authorization, String jobHandle) {
    return withMandatoryBearer(
        authorization,
        token -> {
          long jobId = resolveJobId(appId, terId, token, jobHandle, "file");
          try {
            StreamedHttpResponse upstream = upstreamClient.getFile(jobId);
            if (upstream.response().code() == 401 || upstream.response().code() == 403) {
              upstream.close();
              return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                  .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                  .body(
                      org.sitmun.proxy.middleware.dto.ProxyProblemResponses
                          .upstreamAuthorizationFailure());
            }
            if (!upstream.response().isSuccessful()) {
              upstream.close();
              return upstreamUnavailable();
            }
            HttpHeaders headers = new HttpHeaders();
            copyHeader(upstream, headers, HttpHeaders.CONTENT_TYPE);
            copyHeader(upstream, headers, HttpHeaders.CONTENT_LENGTH);
            copyHeader(upstream, headers, HttpHeaders.CONTENT_DISPOSITION);
            if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
              headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            StreamingResponseBody stream =
                output -> {
                  try (upstream) {
                    upstream.bodyStream().transferTo(output);
                  }
                };
            return ResponseEntity.status(upstream.response().code()).headers(headers).body(stream);
          } catch (IOException e) {
            log.error("MBTiles file download failed with {}", e.getClass().getSimpleName());
            return upstreamUnavailable();
          }
        });
  }

  private long resolveJobId(int appId, int terId, String token, String jobHandle, String action) {
    String principalHint = principalExtractor.extractSub(token).orElse(null);
    Optional<JobHandleClaims> early =
        principalHint == null
            ? Optional.empty()
            : jobHandleService.verify(jobHandle, principalHint, appId, terId);

    BackendConfigResponse config =
        authorize(
            new BackendConfigRequest(appId, terId, action, jobHandle, null, null, null, null, null),
            token);

    String principal = StringUtils.hasText(principalHint) ? principalHint : config.username();
    JobHandleClaims claims =
        early.orElseGet(
            () ->
                jobHandleService
                    .verify(jobHandle, principal, appId, terId)
                    .orElseThrow(
                        () ->
                            new MbtilesClientException(
                                400, "Invalid or expired MBTiles job handle")));
    return claims.jobId();
  }

  private BackendConfigResponse authorize(BackendConfigRequest request, String token) {
    return configClient.authorize(request, token);
  }

  private ResponseEntity<?> withMandatoryBearer(String authorization, TokenHandler handler) {
    AuthorizationToken parsed = AuthorizationBearerParser.parse(authorization, true);
    if (!parsed.valid()) {
      return authorization == null ? missingAuthorizationHeader() : invalidAuthorizationHeader();
    }
    try {
      return handler.handle(parsed.token());
    } catch (MbtilesClientException e) {
      return proxyInvalidRequest(e.getMessage());
    } catch (MbtilesBackendException e) {
      if (e.clientError().getStatusCode().value() == 401
          || e.clientError().getStatusCode().value() == 403) {
        return backendAuthorizationFailure(e.clientError(), objectMapper);
      }
      return ResponseEntity.status(e.clientError().getStatusCode().value())
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              org.sitmun.proxy.middleware.dto.ProblemDetail.builder()
                  .type(org.sitmun.proxy.middleware.dto.ProblemTypes.PROXY_BACKEND_ERROR)
                  .status(e.clientError().getStatusCode().value())
                  .title("Backend Error")
                  .detail("Backend configuration request failed")
                  .instance("/proxy")
                  .build());
    }
  }

  private static void copyHeader(StreamedHttpResponse upstream, HttpHeaders headers, String name) {
    String value = upstream.response().header(name);
    if (value != null && isForwardableResponseHeader(name)) {
      headers.add(name, value);
    }
  }

  private static ResponseEntity<?> upstreamUnavailable() {
    return backendUnavailable();
  }

  @FunctionalInterface
  private interface TokenHandler {
    ResponseEntity<?> handle(String token);
  }
}
