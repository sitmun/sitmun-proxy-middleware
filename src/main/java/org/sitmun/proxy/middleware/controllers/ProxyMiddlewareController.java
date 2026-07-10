package org.sitmun.proxy.middleware.controllers;

import static org.sitmun.proxy.middleware.dto.ProxyProblemResponses.invalidAuthorizationHeader;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Pattern;
import org.sitmun.proxy.middleware.service.RequestConfigurationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/proxy")
public class ProxyMiddlewareController {

  private static final Pattern BEARER_AUTHORIZATION =
      Pattern.compile("(?i)^Bearer ([A-Za-z0-9\\-._~+/]+=*)$");

  private final RequestConfigurationService requestConfigurationService;

  public ProxyMiddlewareController(RequestConfigurationService requestConfigurationService) {
    this.requestConfigurationService = requestConfigurationService;
  }

  @GetMapping("/{appId}/{terId}/{type}/{typeId}")
  public ResponseEntity<?> getService(
      @PathVariable Integer appId,
      @PathVariable Integer terId,
      @PathVariable String type,
      @PathVariable Integer typeId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestParam(required = false) Map<String, String> params,
      HttpServletRequest request) {
    AuthorizationToken authorizationToken = parseAuthorization(authorization);
    if (!authorizationToken.valid()) {
      return invalidAuthorizationHeader();
    }
    String url = request.getRequestURL().toString();
    return requestConfigurationService.doRequest(
        appId, terId, type, typeId, authorizationToken.token(), params, url, null);
  }

  @PostMapping(
      value = "/{appId}/{terId}/{type}/{typeId}",
      consumes = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<?> postService(
      @PathVariable Integer appId,
      @PathVariable Integer terId,
      @PathVariable String type,
      @PathVariable Integer typeId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestParam(required = false) Map<String, String> params,
      HttpServletRequest request,
      @RequestBody(required = false) String body) {
    AuthorizationToken authorizationToken = parseAuthorization(authorization);
    if (!authorizationToken.valid()) {
      return invalidAuthorizationHeader();
    }
    String url = request.getRequestURL().toString();
    return requestConfigurationService.doRequest(
        appId, terId, type, typeId, authorizationToken.token(), params, url, body);
  }

  private static AuthorizationToken parseAuthorization(String authorization) {
    if (authorization == null) {
      return new AuthorizationToken(true, null);
    }
    var matcher = BEARER_AUTHORIZATION.matcher(authorization);
    return matcher.matches()
        ? new AuthorizationToken(true, matcher.group(1))
        : new AuthorizationToken(false, null);
  }

  private record AuthorizationToken(boolean valid, String token) {}
}
