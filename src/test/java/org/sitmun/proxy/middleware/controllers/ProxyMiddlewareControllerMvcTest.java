package org.sitmun.proxy.middleware.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.service.RequestConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProxyMiddlewareController.class)
class ProxyMiddlewareControllerMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RequestConfigurationService requestConfigurationService;

  @Test
  void malformedBearerReturnsSanitizedProblemContract() throws Exception {
    mockMvc
        .perform(get("/proxy/1/2/WMS/3").header(HttpHeaders.AUTHORIZATION, "Bearer first second"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
        .andExpect(jsonPath("$.type").value("https://sitmun.org/problems/proxy-invalid-request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid Authorization Header"))
        .andExpect(jsonPath("$.detail").value("Authorization header must contain one Bearer token"))
        .andExpect(jsonPath("$.instance").value("/proxy"))
        .andExpect(jsonPath("$.properties.origin").value("proxy-request"));

    verifyNoInteractions(requestConfigurationService);
  }
}
