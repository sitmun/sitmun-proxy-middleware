package org.sitmun.proxy.middleware.controllers;

import java.util.Map;

import org.sitmun.proxy.middleware.service.ProxyMiddlewareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proxy")
public class ProxyMiddlewareController {
	
	@Autowired
	private ProxyMiddlewareService proxyMiddlewareService;

	@GetMapping("/{appId}/{terId}/{type}/{typeId}")
	public ResponseEntity<?> getService(@PathVariable("appId") Integer appId, @PathVariable("terId") Integer terId,
			@PathVariable("type") String type, @PathVariable("typeId") Integer typeId,
			@RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String authorization, @RequestParam(required=false) Map<String, String> params){
		String token = authorization != null ? authorization.substring(7) : null;
		return proxyMiddlewareService.doRequest(appId, terId, type, typeId, token, params);
	}
}
