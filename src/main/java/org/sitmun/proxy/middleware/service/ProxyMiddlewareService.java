package org.sitmun.proxy.middleware.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.ConfigProxyRequest;
import org.sitmun.proxy.middleware.dto.DatasourcePayloadDto;
import org.sitmun.proxy.middleware.dto.ErrorResponseDTO;
import org.sitmun.proxy.middleware.dto.OgcWmsPayloadDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
	private JdbcUtils jdbcUtils;
	
	@Autowired
	private MapServiceUtils mapServiceUtils;
	
	@Value("${security.authentication.middleware.secret}")
	private String secret;
	
	public ResponseEntity<?> doRequest(Integer appId, Integer terId, String type,
			Integer typeId, String token, Map<String, String> params){
		ConfigProxyRequest configProxyRequest = new ConfigProxyRequest(appId, terId, type, typeId, "GET", params, null, token);
		ResponseEntity<?> response = configRequest(configProxyRequest);
		if (response.getStatusCodeValue() == 200){
			ConfigProxyDto configProxyDto = (ConfigProxyDto) response.getBody();
			if(configProxyDto != null && configProxyDto.getPayload() instanceof OgcWmsPayloadDto) {
				return serviceRequest(configProxyDto);
			} else if(configProxyDto != null && configProxyDto.getPayload() instanceof DatasourcePayloadDto) {
				return jdbcRequest(configProxyDto);
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
	
	private ResponseEntity<?> serviceRequest(ConfigProxyDto configProxyDto) {
		OgcWmsPayloadDto payload = (OgcWmsPayloadDto)configProxyDto.getPayload();
		HttpResponse<InputStream> response = mapServiceUtils.doRequest(payload);
		try {
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(response.headers().allValues("Content-type").get(0)))
					.body(response.body().readAllBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private ResponseEntity<?> jdbcRequest(ConfigProxyDto configProxyDto) {
		DatasourcePayloadDto payload = (DatasourcePayloadDto)configProxyDto.getPayload();
		List<Map<String, Object>> queryResult = jdbcUtils.doQuery(payload);
		
		return ResponseEntity.ok().body(queryResult);
	}
}
