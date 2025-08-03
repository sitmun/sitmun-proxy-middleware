package org.sitmun.proxy.middleware.controllers;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.sitmun.proxy.middleware.service.RequestConfigurationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/proxy")
public class ProxyMiddlewareController {

  private final RequestConfigurationService requestConfigurationService;

  public ProxyMiddlewareController(RequestConfigurationService requestConfigurationService) {
    this.requestConfigurationService = requestConfigurationService;
  }

  @GetMapping("/{appId}/{terId}/{type}/{typeId}")
  public ResponseEntity<?> getService(
      @PathVariable("appId") Integer appId,
      @PathVariable("terId") Integer terId,
      @PathVariable("type") String type,
      @PathVariable("typeId") Integer typeId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestParam(required = false) Map<String, String> params,
      HttpServletRequest request) {
    String token = authorization != null ? authorization.substring(7) : null;
    String url = request.getRequestURL().toString();
    return requestConfigurationService.doRequest(appId, terId, type, typeId, token, params, url);
  }
}
