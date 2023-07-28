package org.sitmun.proxy.middleware.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.sitmun.proxy.middleware.test.dto.AuthenticationResponse;
import org.sitmun.proxy.middleware.test.dto.UserPasswordAuthenticationRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TestUtils {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "admin";

  public static String requestAuthorization(MockMvc mvc) throws Exception {
    UserPasswordAuthenticationRequest login = UserPasswordAuthenticationRequest.builder()
      .username(ADMIN_USERNAME)
      .password(ADMIN_PASSWORD)
      .build();
    ObjectMapper mapper = new ObjectMapper();
    String result = "";
    String authorization = mvc.perform(post(URIConstants.AUTHORIZATION_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(login)))
      .andReturn().getResponse().getContentAsString();

    if (authorization.contains("id_token")) {
      result = (String) new JSONObject(authorization).get("id_token");
    }
    return "Bearer " + result;
  }

  public static String requestAuthorization(RestTemplate restTemplate) {
    UserPasswordAuthenticationRequest login = UserPasswordAuthenticationRequest.builder()
      .username(ADMIN_USERNAME)
      .password(ADMIN_PASSWORD)
      .build();
    ResponseEntity<AuthenticationResponse> loginResponse =
      restTemplate
        .postForEntity(URIConstants.AUTHORIZATION_URI, login, AuthenticationResponse.class);
    Assertions.assertThat(loginResponse.getBody()).isNotNull();
    return "Bearer " + loginResponse.getBody().getIdToken();
  }
}
