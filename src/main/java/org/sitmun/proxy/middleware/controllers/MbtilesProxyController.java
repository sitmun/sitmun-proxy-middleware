package org.sitmun.proxy.middleware.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.ClientCreateRequest;
import org.sitmun.proxy.middleware.mbtiles.MbtilesProxyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proxy")
public class MbtilesProxyController {

  private final MbtilesProxyService mbtilesProxyService;
  private final ObjectMapper objectMapper;

  public MbtilesProxyController(
      MbtilesProxyService mbtilesProxyService, ObjectMapper objectMapper) {
    this.mbtilesProxyService = mbtilesProxyService;
    this.objectMapper = objectMapper;
  }

  @PostMapping(
      value = "/{appId}/{terId}/mbtiles/estimate",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> estimate(
      @PathVariable Integer appId,
      @PathVariable Integer terId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody String rawBody)
      throws Exception {
    ClientCreateRequest body = objectMapper.readValue(rawBody, ClientCreateRequest.class);
    return mbtilesProxyService.estimate(appId, terId, authorization, rawBody, body);
  }

  @PostMapping(value = "/{appId}/{terId}/mbtiles", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> create(
      @PathVariable Integer appId,
      @PathVariable Integer terId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody String rawBody)
      throws Exception {
    ClientCreateRequest body = objectMapper.readValue(rawBody, ClientCreateRequest.class);
    return mbtilesProxyService.create(appId, terId, authorization, rawBody, body);
  }

  @GetMapping("/{appId}/{terId}/mbtiles/{jobHandle}")
  public ResponseEntity<?> status(
      @PathVariable Integer appId,
      @PathVariable Integer terId,
      @PathVariable String jobHandle,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    return mbtilesProxyService.status(appId, terId, authorization, jobHandle);
  }

  @GetMapping("/{appId}/{terId}/mbtiles/{jobHandle}/file")
  public ResponseEntity<?> file(
      @PathVariable Integer appId,
      @PathVariable Integer terId,
      @PathVariable String jobHandle,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    return mbtilesProxyService.file(appId, terId, authorization, jobHandle);
  }
}
