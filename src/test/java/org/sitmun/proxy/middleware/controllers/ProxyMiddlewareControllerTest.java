package org.sitmun.proxy.middleware.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.sitmun.proxy.middleware.test.TestUtils;
import org.sitmun.proxy.middleware.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class ProxyMiddlewareControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private RestTemplate restTemplate;

  private String token;

  @BeforeAll
  void setup() {
    this.token = TestUtils.requestAuthorization(restTemplate);
  }

  /**
   * Acceso con usuario público a servicio WMS público
   *
   * @throws Exception
   */
  @Test
  void publicWmsPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/773")
        .concat("?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=XDE50_DB&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256")))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario público a servicio WFS público
   *
   * @throws Exception
   */
  @Test
  void publicWfsPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/2")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario autenticado a servicio WMS público
   *
   * @throws Exception
   */
  @Test
  void publicWmsOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/773")
        .concat("?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=XDE50_DB&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario autenticado a servicio WFS público
   *
   * @throws Exception
   */
  @Test
  void publicWfsOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/2")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario público a servicio WMS público a servicios que no ofrece HTTPS
   *
   * @throws Exception
   */
  @Test
  void publicWmsHttpPublicUSer() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/3")
        .concat("?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734")))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario público a servicio WFS público a servicios que no ofrece HTTPS
   *
   * @throws Exception
   */
  @Test
  void publicWfsHttpPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario autenticado a servicio WMS público a servicios que no ofrecen HTTPS
   *
   * @throws Exception
   */
  @Test
  void publicWmsHttpOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/3")
        .concat("?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario autenticado a servicio WFS público a servicios que no ofrecen HTTPS
   *
   * @throws Exception
   */
  @Test
  void publicWfsHttpOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario público a servicio WMS privado con autenticación básica
   *
   * @throws Exception
   */
  @Test
  void privateWmsBasicAuthenticationPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/5")
        .concat("?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734")))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario público a servicio WFS privado con autenticación básica
   *
   * @throws Exception
   */
  @Test
  void privateWfsBasicAuthenticationPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/6")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario autenticado a servicio WMS privado con autenticación básica
   *
   * @throws Exception
   */
  @Test
  void privateWmsBasicAuthenticationOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/5")
        .concat("?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario autenticado a servicio WFS privado con autenticación básica
   *
   * @throws Exception
   */
  @Test
  void privateWfsBasicAuthenticationOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/6")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario público a servicio WMS privado con IP en red privada
   *
   * @throws Exception
   */
  @Test
  void privateWmsIpPrivateRedPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/7")
        .concat("?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734")))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario público a servicio WFS privado con IP en red privada
   *
   * @throws Exception
   */
  @Test
  void privateWfsIpPrivateRedPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario autenticado a servicio WMS privado con IP en red privada
   *
   * @throws Exception
   */
  @Test
  void privateWmsIpPrivateRedOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/7")
        .concat("?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
  }

  /**
   * Acceso con usuario autenticado a servicio WFS privado con IP en red privada
   *
   * @throws Exception
   */
  @Test
  void privateWfsIpPrivateRedOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8")
        .concat("?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.features", hasSize(25)));
  }

  /**
   * Acceso con usuario público a servicio WMS privado añadiendo un filtro fijo en la petición
   *
   * @throws Exception
   */
  @Test
  void privateWmsWithFixedFilterPublicUser() throws Exception {

  }

  /**
   * Acceso con usuario público a servicio WFS privado añadiendo un filtro fijo en la petición
   *
   * @throws Exception
   */
  @Test
  void privateWfsWithFixedFilterPublicUser() throws Exception {

  }

  /**
   * Acceso con usuario autenticado a servicio WMS privado añadiendo un filtro fijo en la petición
   *
   * @throws Exception
   */
  @Test
  void privateWmsWithFixedFilterOtherUser() throws Exception {

  }

  /**
   * Acceso con usuario autenticado a servicio WFS privado añadiendo un filtro fijo en la petición
   *
   * @throws Exception
   */
  @Test
  void privateWfsWithFixedFilterOtherUser() throws Exception {

  }

  @Test
  void privateWmsWithVaryFilterPublicUser() throws Exception {

  }

  @Test
  void privateWfsWithVaryFilterPublicUser() throws Exception {

  }

  @Test
  void privateWmsWithVaryFilterOtherUser() throws Exception {

  }

  @Test
  void privateWfsWithVaryFilterOtherUser() throws Exception {

  }

  /**
   * Acceso con usuario público a un servicio relacional
   *
   * @throws Exception
   */
  @Test
  void jdbcAccessPublicUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/SQL/3279")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(3)));
  }

  /**
   * Acceso con usuario autenticado a un servicio relacional
   *
   * @throws Exception
   */
  @Test
  void jdbcAccessOtherUser() throws Exception {
    mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/SQL/3279"))
        .header("Authorization", this.token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(3)));
  }

  /**
   * Acceso con usuario público a un servicio relacional con filtrado
   *
   * @throws Exception
   */
  @Test
  void jdbcAccessWithFiltersPublicUser() throws Exception {

  }

  /**
   * Acceso con usuario autenticado a un servicio relacional con filtrado
   *
   * @throws Exception
   */
  @Test
  void jdbcAccessWithFiltersOtherUser() throws Exception {

  }

}
