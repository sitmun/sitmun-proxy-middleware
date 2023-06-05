package org.sitmun.proxy.middleware.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.sitmun.proxy.middleware.request.HttpRequest;
import org.sitmun.proxy.middleware.service.GlobalRequestService;
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
  private GlobalRequestService requestService;

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
	 * Public user access to the public WMS service.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWmsPublicUser() throws Exception {
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=DTE50_MUN&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256")))
				.andExpect(status().isOk()).andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
	}

	/**
	 * Public user access to the public WFS service.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWfsPublicUser() throws Exception {
		// TODO: Replace the geoinnfo ID with one that corresponds to a WFS service
		// accessible to a sitmun public user.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/2").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.features", hasSize(25)));
	}

	/**
	 * Authenticated user access to the public WMS service.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWmsOtherUser() throws Exception {
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=DTE50_MUN&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256"))
						.header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
	}

	/**
	 * Authenticated user access to the public WFS service.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWfsOtherUser() throws Exception {
		// TODO: Replace the geoifo ID with one that corresponds to a WFS service
		// requiring Sitmun authentication.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/2").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
						.header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.features", hasSize(25)));
	}

	/**
	 * Public user access to public WMS services that do not provide HTTPS.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWmsHttpPublicUSer() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WMS service that
		// does not support HTTPS and can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/3").concat(
				"?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734")))
				.andExpect(status().isOk()).andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
	}

	/**
	 * Public user access to public WFS services that do not provide HTTPS.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWfsHttpPublicUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WFS service that
		// does not support HTTPS and can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.features", hasSize(25)));
	}

	/**
	 * Authenticated user access to public WMS services that do not provide HTTPS.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWmsHttpOtherUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WMS
		// service that does not accept HTTPS and can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/3").concat(
				"?REQUEST=GetMap&FORMAT=image/png&LAYERS=DT50_MUN&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734"))
						.header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
	}

	/**
	 * Authenticated user access to public WFS services that do not provide HTTPS.
	 *
	 * @throws Exception
	 */
	@Test
	void publicWfsHttpOtherUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WFS
		// service that does not accept HTTPS and can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
						.header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.features", hasSize(25)));
	}

	/**
	 * Public user access to a private WMS service with basic authentication.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWmsBasicAuthenticationPublicUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WMS service with
		// basic authentication that can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/3").concat("?REQUEST=GetCapabilities&VERSION=1.3.0")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a private WFS service with basic authentication.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWfsBasicAuthenticationPublicUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WFS service with
		// basic authentication that can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/6").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WMS service with basic authentication.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWmsBasicAuthenticationOtherUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WMS service with
		// basic authentication and requires Sitmun authentication.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/3").concat("?REQUEST=GetCapabilities&VERSION=1.3.0"))
				.header("Authorization", this.token)).andExpect(status().isOk())
				.andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WFS service with basic authentication.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWfsBasicAuthenticationOtherUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WFS service with
		// basic authentication and requires Sitmun authentication.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/6").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
						.header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a private WMS service with an IP on a private network.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWmsIpPrivateRedPublicUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WMS
		// service with basic authentication located on a private network, which can be
		// accessed by a public user from Sitmun.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/7").concat(
				"?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a private WFS service with an IP on a private network.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWfsIpPrivateRedPublicUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WFS
		// service with basic authentication located on a private network, which can be
		// accessed by a public user from Sitmun.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WMS service with an IP on a private
	 * network.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWmsIpPrivateRedOtherUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WMS
		// service with basic authentication, located on a private network, and
		// requiring authentication from a Sitmun user.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/7").concat(
				"?REQUEST=GetMap&FORMAT=image/png&LAYERS=nombreLayer&WIDTH=256&HEIGHT=256&SRS=EPSG:25830&BBOX=519828.9837386483,4110600.554306189,519894.9813798326,4110666.5519473734"))
						.header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WFS service with an IP on a private
	 * network.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWfsIpPrivateRedOtherUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WFS
		// service with basic authentication, located on a private network, and
		// requiring authentication from a Sitmun user.
		// TODO: Adapt the parameters to align with the corresponding service.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json"))
						.header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a private WMS service, adding a fixed filter to the
	 * request.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWmsWithFixedFilterPublicUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WMS
		// service with basic authentication, accessible as a public user of Sitmun.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=DTE50_MUN&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a private WFS service, adding a fixed filter to the
	 * request.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWfsWithFixedFilterPublicUser() throws Exception {
		// TODO: Replace the ID of the geoinfo with one that corresponds to a WFS
		// service with basic authentication, accessible as a public user of Sitmun.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WMS service, adding a fixed filter to
	 * the request.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWmsWithFixedFilterOtherUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WMS service with
		// basic authentication and requires Sitmun authentication.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=DTE50_MUN&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WFS service, adding a fixed filter to
	 * the request.
	 *
	 * @throws Exception
	 */
	@Test
	void privateWfsWithFixedFilterOtherUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WFS service with
		// basic authentication and requires Sitmun authentication.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a private WMS service, adding a varying filter to the
	 * request.
	 * 
	 * @throws Exception
	 */
	@Test
	void privateWmsWithVaryFilterPublicUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WMS service with
		// basic authentication that can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=DTE50_MUN&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a private WFS service, adding a varying filter to the
	 * request.
	 * 
	 * @throws Exception
	 */
	@Test
	void privateWfsWithVaryFilterPublicUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WFS service with
		// basic authentication that can be accessed by a public user.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WMS service, adding a varying filter
	 * to the request.
	 * 
	 * @throws Exception
	 */
	@Test
	void privateWmsWithVaryFilterOtherUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WMS service with
		// basic authentication and requires Sitmun authentication.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/4").concat(
				"?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&LAYERS=DTE50_MUN&SRS=EPSG%3A4326&BBOX=2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519&styles=&width=256&height=256")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Authenticated user access to a private WFS service, adding a varying filter
	 * to the request.
	 * 
	 * @throws Exception
	 */
	@Test
	void privateWfsWithVaryFilterOtherUser() throws Exception {
		// TODO: Replace the geoinfo ID with one that corresponds to a WFS service with
		// basic authentication and requires Sitmun authentication.
		// TODO: Adapt the parameters to align with the corresponding service, including
		// the filtering parameters.
		// TODO: Change the content to the corresponding username:password values.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/GEO/8").concat(
				"?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&typeNames=namespace:layerName&outputFormat=application/json")))
				.andExpect(status().isOk()).andExpect(content().string("userServ:passwordServ"));
	}

	/**
	 * Public user access to a relational service.
	 *
	 * @throws Exception
	 */
	@Test
	void jdbcAccessPublicUser() throws Exception {
		// TODO: Replace the task ID with one that can be accessed by a public user.
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/SQL/3279"))).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
	}

	/**
	 * Authenticated user access to a relational service.
	 *
	 * @throws Exception
	 */
	@Test
	void jdbcAccessOtherUser() throws Exception {
		// TODO: Replace the task ID with one that requires Sitmun authentication.
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/SQL/3279")).header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
	}

	/**
	 * Public user access to a relational service filtered.
	 *
	 * @throws Exception
	 */
	@Test
	void jdbcAccessWithFiltersPublicUser() throws Exception {
		// TODO: Change the task ID to one that accepts filters for a public sitmun user.
		// TODO: Add filters to the request
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/SQL/3279")).header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
	}

	/**
	 * Authenticated user access to a relational service filtered.
	 *
	 * @throws Exception
	 */
	@Test
	void jdbcAccessWithFiltersOtherUser() throws Exception {
		// TODO: Change the task ID to one that accepts filters for a authenticated sitmun user.
		// TODO: Add filters to the request
		// TODO: Change the value of hasSize to the appropriate one.
		mvc.perform(get(URIConstants.PROXY_URI.concat("1/0/SQL/3279")).header("Authorization", this.token))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
	}

}
