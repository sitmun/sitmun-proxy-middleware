package org.sitmun.proxy.middleware.service;

import okhttp3.Request;
import okhttp3.Response;

public interface ClientService {

	public Response executeRequest(Request httpRequest);
}
