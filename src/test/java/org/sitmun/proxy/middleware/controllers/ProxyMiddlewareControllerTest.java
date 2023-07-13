package org.sitmun.proxy.middleware.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.sitmun.proxy.middleware.service.GlobalRequestService;
import org.sitmun.proxy.middleware.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;

import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@TestInstance(Lifecycle.PER_CLASS)
class ProxyMiddlewareControllerTest {

  @Autowired
  private GlobalRequestService globalRequestService;
  
  private JacksonTester<Object> jsonTester;

  @BeforeAll
  public void setup() {
      ObjectMapper objectMapper = new ObjectMapper();
      JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Public user access to the public WMS service.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  void publicWms() throws Exception {
	int statusCode = globalRequestService.executeRequest(TestUtils.createFakeWmsPayload(false)).getStatusCodeValue();
	assertThat(statusCode).isEqualTo(200);
	
    //.andExpect(status().isOk()).andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Public user access to the public WFS service.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  void publicWfs() throws Exception {
	String response = new String((byte[])globalRequestService.executeRequest(TestUtils.createFakeWfsPayload(false)).getBody(), "UTF-8");
	assertThat(jsonTester.parse(response))
	  .extracting("totalFeatures").isEqualTo(268);
  }

  /**
   * Public user access to a private WMS service with basic authentication.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  void privateWmsBasicAuthentication() throws Exception {
    Object response = globalRequestService.executeRequest(TestUtils.createFakeWmsPayload(true)).getBody();
    assertThat(new String((byte[])response)).isEqualTo("userServ:passwordServ");
  }

  /**
   * Public user access to a private WMS service with an IP on a private network.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  void privateWmsIpPrivateRed() throws Exception {
    int statusCode = globalRequestService.executeRequest(TestUtils.createFakePrivateIpWmsPayload()).getStatusCodeValue();
    assertThat(statusCode).isEqualTo(200);
  }

  /**
   * Public user access to a private WMS service, adding a filter to the
   * request.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  void privateWfsWithFilter() throws Exception {
	String response = new String((byte[])globalRequestService.executeRequest(TestUtils.createFakeWfsPayload(true)).getBody(), "UTF-8");
	assertThat(jsonTester.parse(response))
	  .extracting("totalFeatures").isEqualTo(1335);
  }

  /**
   * Public user access to a relational service.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  void jdbcAccess() throws Exception {
    List<?> response = (List)globalRequestService.executeRequest(TestUtils.createFakeDatasourcePayload(false))
     .getBody();
    System.out.println(response);
    assertThat(response).hasSize(33);
  }

  /**
   * Public user access to a relational service filtered.
   * probablemente no haga falta
   * @throws Exception for unexpected failures
   */
  @Test
  void jdbcAccessWithFilters() throws Exception {
	List<?> response = (List)globalRequestService.executeRequest(TestUtils.createFakeDatasourcePayload(true))
	 .getBody();
	assertThat(response).hasSize(4);
  }

}
