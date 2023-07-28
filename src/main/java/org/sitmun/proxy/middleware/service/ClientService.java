package org.sitmun.proxy.middleware.service;

import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public interface ClientService {

  Response executeRequest(Request httpRequest) throws IOException;
}
