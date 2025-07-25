package org.sitmun.proxy.middleware.controllers;

import org.sitmun.proxy.middleware.request.TileRequestDto;
import org.sitmun.proxy.middleware.response.Response;
import org.sitmun.proxy.middleware.service.MBTilesService;
import org.sitmun.proxy.middleware.service.ProxyMiddlewareService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/proxy")
public class ProxyMiddlewareController {

  private final ProxyMiddlewareService proxyMiddlewareService;

  private final MBTilesService mbTilesService;

  public ProxyMiddlewareController(ProxyMiddlewareService proxyMiddlewareService,
    MBTilesService mbTilesService) {
    this.proxyMiddlewareService = proxyMiddlewareService;
    this.mbTilesService = mbTilesService;
  }

  @GetMapping("/{appId}/{terId}/{type}/{typeId}")
  public ResponseEntity<?> getService(@PathVariable("appId") Integer appId,
                                      @PathVariable("terId") Integer terId,
                                      @PathVariable("type") String type,
                                      @PathVariable("typeId") Integer typeId,
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                      @RequestParam(required = false) Map<String, String> params,
                                      HttpServletRequest request) {
    String token = authorization != null ? authorization.substring(7) : null;
    String url = request.getRequestURL().toString();
    return proxyMiddlewareService.doRequest(appId, terId, type, typeId, token, params, url);
  }

  @PostMapping(value = "/mbtiles", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<?> startMBTilesJob(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @RequestBody TileRequestDto tileRequest) throws Exception {
    String token = authorization != null ? authorization.substring(7) : null;
    
    Long jobId = mbTilesService.startJob(tileRequest);

    Response<String> response = new Response<>("", 200, MediaType.TEXT_PLAIN_VALUE, jobId.toString());
    return response.asResponseEntity();
  }

  @PostMapping(value = "/mbtiles/estimate", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> estimateMBTilesSize(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @RequestBody TileRequestDto tileRequest) throws Exception {
    String token = authorization != null ? authorization.substring(7) : null;
    return mbTilesService.estimateSize(tileRequest);
  }
  
  @GetMapping(value = "/mbtiles/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getMBTilesJobStatus(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                               @PathVariable Long jobId) {
    String token = authorization != null ? authorization.substring(7) : null;
    return mbTilesService.getJobStatus(jobId);
  }

  @GetMapping(value = "/mbtiles/{jobId}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> downloadFile(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                               @PathVariable Long jobId) throws IOException {
    String token = authorization != null ? authorization.substring(7) : null;
    return mbTilesService.getMBTilesFile(jobId);
  }
}
