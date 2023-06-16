package org.sitmun.proxy.middleware.service;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

public interface ClientService {

	Response executeRequest(Request httpRequest) throws IOException;
}
