package org.sitmun.proxy.middleware.service;

import java.io.IOException;
import java.util.List;

import javax.annotation.Priority;

import org.sitmun.proxy.middleware.interceptors.TestInterceptor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

@Profile("test")
@Priority(1)
@Service
public class ClientServiceTestImpl implements ClientService {

	private final List<TestInterceptor> interceptors;

	private OkHttpClient httpClient;

	public ClientServiceTestImpl(List<TestInterceptor> interceptors) {
		this.interceptors = interceptors;
		Builder builder = new Builder();
		addInterceptors(builder);
		httpClient = builder.build();
	}

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

	private void addInterceptors(Builder builder) {
		if (interceptors != null && !interceptors.isEmpty()) {
			for (TestInterceptor ti : interceptors) {
				builder.addInterceptor(ti);
			}
		}
	}

	public void addInterceptor(Interceptor interceptor) {
		httpClient = new Builder().addInterceptor(interceptor).build();
	}

	public void removeInterceptor(Interceptor interceptor) {
		if (interceptor != null && httpClient.interceptors().contains(interceptor)) {
			Builder builder = httpClient.newBuilder();
			builder.interceptors().remove(interceptor);
			httpClient = builder.build();
		}
	}

}
