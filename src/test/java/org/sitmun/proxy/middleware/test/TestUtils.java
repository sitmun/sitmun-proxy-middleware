package org.sitmun.proxy.middleware.test;

import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TestUtils {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "admin";

  public static String requestAuthorization(MockMvc mvc) throws Exception {
    String result = "";
    String authorization = mvc.perform(post(URIConstants.AUTHORIZATION_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\": \"admin\", \"password\": \"admin\"}"))
      .andReturn().getResponse().getContentAsString();

    if (authorization.contains("id_token")) {
      result = (String) new JSONObject(authorization).get("id_token");
    }
    return "Bearer " + result;
  }

  public static String requestAuthorization(RestTemplate restTemplate) {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername(ADMIN_USERNAME);
    login.setPassword(ADMIN_PASSWORD);
    ResponseEntity<AuthenticationResponse> loginResponse =
      restTemplate
        .postForEntity(URIConstants.AUTHORIZATION_URI, login, AuthenticationResponse.class);
    Assertions.assertThat(loginResponse.getBody()).isNotNull();
    return "Bearer " + loginResponse.getBody().getIdToken();
  }
}
