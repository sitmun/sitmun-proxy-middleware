package org.sitmun.proxy.middleware.controllers;

import org.sitmun.proxy.middleware.service.ProxyMiddlewareService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/proxy")
public class ProxyMiddlewareController {

  private final ProxyMiddlewareService proxyMiddlewareService;

  public ProxyMiddlewareController(ProxyMiddlewareService proxyMiddlewareService) {
    this.proxyMiddlewareService = proxyMiddlewareService;
  }

  @GetMapping("/{appId}/{terId}/{type}/{typeId}")
  public ResponseEntity<?> getService(@PathVariable("appId") Integer appId, @PathVariable("terId") Integer terId,
                                      @PathVariable("type") String type, @PathVariable("typeId") Integer typeId,
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization, @RequestParam(required = false) Map<String, String> params) {
    String token = authorization != null ? authorization.substring(7) : null;
    return proxyMiddlewareService.doRequest(appId, terId, type, typeId, token, params);
  }
}
