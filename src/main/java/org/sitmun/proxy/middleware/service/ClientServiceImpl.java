package org.sitmun.proxy.middleware.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class ClientServiceImpl implements ClientService {

	private final OkHttpClient httpClient = new OkHttpClient();
	

	@Override
	public Response executeRequest(Request httpRequest) {
		Response response = null;
		
		try {
			response = httpClient.newCall(httpRequest).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return response;
	}

}
