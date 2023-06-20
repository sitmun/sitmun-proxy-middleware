package org.sitmun.proxy.middleware.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ClientServiceImpl implements ClientService {

  private final OkHttpClient httpClient = new OkHttpClient();


  @Override
  public Response executeRequest(Request httpRequest) throws IOException {
    return httpClient.newCall(httpRequest).execute();
  }

}
