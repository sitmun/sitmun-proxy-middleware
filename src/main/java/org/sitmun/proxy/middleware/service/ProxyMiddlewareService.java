package org.sitmun.proxy.middleware.service;

import java.util.Date;
import java.util.Map;

import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.ConfigProxyRequest;
import org.sitmun.proxy.middleware.dto.ErrorResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class ProxyMiddlewareService {

	@Value("${sitmun.config.url}")
	private String configUrl;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private GlobalRequestService globalRequestService;
	
	@Value("${security.authentication.middleware.secret}")
	private String secret;
	
	public ResponseEntity<?> doRequest(Integer appId, Integer terId, String type,
			Integer typeId, String token, Map<String, String> params){
		ConfigProxyRequest configProxyRequest = new ConfigProxyRequest(appId, terId, type, typeId, "GET", params, null, token);
		ResponseEntity<?> response = configRequest(configProxyRequest);
		if (response.getStatusCodeValue() == 200){
			ConfigProxyDto configProxyDto = (ConfigProxyDto) response.getBody();
			if(configProxyDto != null) {
				return globalRequestService.executeRequest(configProxyDto.getPayload());
			} else {
				return null;
			}
		} else {
			return response;
		}
	}
	
	private ResponseEntity<?> configRequest(ConfigProxyRequest configRequest){
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("X-SITMUN-Proxy-Key", this.secret);
		HttpEntity<ConfigProxyRequest> httpEntity = new HttpEntity<>(configRequest, requestHeaders);
		ResponseEntity<?> serviceResponse = null;
		try{
			serviceResponse = restTemplate.exchange(configUrl, HttpMethod.POST, httpEntity, ConfigProxyDto.class);
		}catch(HttpClientErrorException e){
			ErrorResponseDTO errorResponse = new ErrorResponseDTO(e.getRawStatusCode(), "", e.getMessage(), configUrl, new Date());
			
			serviceResponse = ResponseEntity.status(e.getStatusCode()).body(errorResponse);
		}
		return serviceResponse;
	}
}
