package org.sitmun.proxy.middleware.protocols.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sitmun.proxy.middleware.service.RequestExecutorResponseImpl;

@DisplayName("WmsCapabilitiesResponseDecorator")
class WmsCapabilitiesResponseDecoratorTest {

  public static final String PROXY_URL = "https://proxy.sitmun.org/middleware/proxy/3/66/WMS/4";

  static final String GEOSERVER = "https://some.domain.com/geoserver";

  static final String SERVICE_URI = GEOSERVER + "/wms?SERVICE=WMS";

  static final String BAD_CAPABILITIES_HOST = "http://localhost:3000";

  private WmsCapabilitiesProperties properties;
  private WmsCapabilitiesResponseDecorator decorator;

  @BeforeEach
  void setUp() {
    properties = new WmsCapabilitiesProperties();
    decorator = new WmsCapabilitiesResponseDecorator(properties);
  }

  private WmsPayloadDto capabilitiesPayload(String uri) {
    return WmsPayloadDto.builder()
        .uri(uri)
        .method("GET")
        .parameters(Map.of("REQUEST", "GetCapabilities", "SERVICE", "WMS"))
        .build();
  }

  private RequestExecutorResponseImpl<byte[]> response(String xmlBody) {
    return new RequestExecutorResponseImpl<>(
        PROXY_URL, 200, "application/xml", xmlBody.getBytes(StandardCharsets.UTF_8));
  }

  private String bodyOf(RequestExecutorResponseImpl<byte[]> r) {
    return new String(r.getBody(), StandardCharsets.UTF_8);
  }

  private static String onlineResourceWrap(String url) {
    return "<OnlineResource xlink:href=\"" + url + "\"/>";
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("primaryReplacementCases")
  void runReplacement(
      String name, String uri, String xmlBody, String shouldContain, String shouldNotContain) {
    final RequestExecutorResponseImpl<byte[]> resp = response(xmlBody);
    decorator.addBehavior(resp, capabilitiesPayload(uri));
    final String body = bodyOf(resp);
    assertThat(body).contains(shouldContain);
    if (shouldNotContain != null) {
      assertThat(body).doesNotContain(shouldNotContain);
    }
  }

  static Stream<Arguments> primaryReplacementCases() {
    return Stream.of(
        Arguments.of(
            "replaces /wfs suffix",
            SERVICE_URI,
            onlineResourceWrap(GEOSERVER + "/wfs?SERVICE=WFS"),
            PROXY_URL + "?SERVICE=WFS",
            null),
        Arguments.of(
            "replaces /wcs suffix",
            SERVICE_URI,
            onlineResourceWrap(GEOSERVER + "/wcs?SERVICE=WCS"),
            PROXY_URL + "?SERVICE=WCS",
            null),
        Arguments.of(
            "replaces /ows suffix",
            SERVICE_URI,
            onlineResourceWrap(GEOSERVER + "/ows?SERVICE=WMS"),
            PROXY_URL + "?SERVICE=WMS",
            null),
        Arguments.of(
            "replaces when single-quoted",
            SERVICE_URI,
            "<OnlineResource xlink:href='" + GEOSERVER + "/wms?SERVICE=WMS'/>",
            PROXY_URL + "?SERVICE=WMS",
            null),
        Arguments.of(
            "removes query string from URI before matching",
            GEOSERVER + "/wms?SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0",
            onlineResourceWrap(GEOSERVER + "/wms?SERVICE=WMS"),
            PROXY_URL + "?SERVICE=WMS",
            null),
        Arguments.of(
            "does NOT replace a partial path match (/geoserver-v2)",
            GEOSERVER + "/wms",
            onlineResourceWrap("https://some.domain.com/geoserver-v2/wms?SERVICE=WMS"),
            "https://some.domain.com/geoserver-v2/wms?SERVICE=WMS",
            null),
        Arguments.of(
            "does NOT replace a URL from a different hostname if not defined in extra sources",
            GEOSERVER + "/wms",
            onlineResourceWrap("https://other.domain.com/geoserver/wms?SERVICE=WMS"),
            "https://other.domain.com/geoserver/wms?SERVICE=WMS",
            null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("extraSourceCases")
  void extraSourceReplacement(
      String name,
      List<String> extraSources,
      String payloadUri,
      String xmlBody,
      String shouldContain) {
    properties.setExtraSources(extraSources);
    final RequestExecutorResponseImpl<byte[]> resp = response(xmlBody);
    decorator.addBehavior(resp, capabilitiesPayload(payloadUri));
    assertThat(bodyOf(resp)).contains(shouldContain);
  }

  static Stream<Arguments> extraSourceCases() {
    return Stream.of(
        Arguments.of(
            "replaces extra source with /wms suffix",
            List.of(BAD_CAPABILITIES_HOST),
            SERVICE_URI,
            onlineResourceWrap(BAD_CAPABILITIES_HOST + "/wms?SERVICE=WMS"),
            PROXY_URL + "?SERVICE=WMS"),
        Arguments.of(
            "replaces extra source without OGC suffix",
            List.of(BAD_CAPABILITIES_HOST),
            SERVICE_URI,
            onlineResourceWrap(BAD_CAPABILITIES_HOST + "?SERVICE=WMS"),
            PROXY_URL + "?SERVICE=WMS"),
        Arguments.of(
            "normalizes extra source with trailing OGC suffix before matching",
            List.of(BAD_CAPABILITIES_HOST + "/geoserver/wms"),
            SERVICE_URI,
            onlineResourceWrap(BAD_CAPABILITIES_HOST + "/geoserver/wfs?SERVICE=WFS"),
            PROXY_URL + "?SERVICE=WFS"),
        Arguments.of(
            "does NOT replace unquoted extra source occurrence",
            List.of(BAD_CAPABILITIES_HOST),
            GEOSERVER + "/wms",
            "<Description>See " + BAD_CAPABILITIES_HOST + "/wms for details</Description>",
            BAD_CAPABILITIES_HOST + "/wms"));
  }

  @Test
  @DisplayName("replaces all occurrences of multiple extra sources in the same document")
  void replacesMultipleExtraSources() {
    properties.setExtraSources(
        List.of(BAD_CAPABILITIES_HOST, "http://192.168.1.10:8080/geoserver"));
    final RequestExecutorResponseImpl<byte[]> resp =
        response(
            "<a href=\""
                + BAD_CAPABILITIES_HOST
                + "/wms?SERVICE=WMS\"/>"
                + "<b href=\"http://192.168.1.10:8080/geoserver/wms?SERVICE=WMS\"/>");
    decorator.addBehavior(resp, capabilitiesPayload(SERVICE_URI));
    assertThat(bodyOf(resp))
        .doesNotContain(BAD_CAPABILITIES_HOST)
        .doesNotContain("http://192.168.1.10:8080/geoserver")
        .contains(PROXY_URL + "?SERVICE=WMS");
  }

  @Test
  @DisplayName(
      "replaces both the primary source and the extra source when both appear in the same document")
  void replacesBothPrimaryAndExtraSource() {
    properties.setExtraSources(List.of(BAD_CAPABILITIES_HOST));
    final RequestExecutorResponseImpl<byte[]> resp =
        response(
            "<Get>"
                + onlineResourceWrap(GEOSERVER + "/wms?SERVICE=WMS")
                + "</Get>"
                + "<Post>"
                + onlineResourceWrap(BAD_CAPABILITIES_HOST + "/wms?SERVICE=WMS")
                + "</Post>");
    decorator.addBehavior(resp, capabilitiesPayload(SERVICE_URI));
    assertThat(bodyOf(resp))
        .doesNotContain(GEOSERVER)
        .doesNotContain(BAD_CAPABILITIES_HOST)
        .contains(PROXY_URL + "?SERVICE=WMS");
  }

  @Test
  @DisplayName("replaces all quoted URLs in a full WMS GetCapabilities response")
  void fullWmsCapabilitiesExample() {
    String inputXml =
        String.format(
            """
      <?xml version="1.0" encoding="UTF-8"?>
      <WMS_Capabilities version="1.3.0"
      	xmlns:xlink="http://www.w3.org/1999/xlink">
      	<Service>
      		<Name>WMS</Name>
      		<Title>My GeoServer</Title>
      		<OnlineResource xlink:href="%s?SERVICE=WMS"/>
      	</Service>
      	<Capability>
      		<Request>
      			<GetCapabilities>
      				<DCPType>
      					<HTTP>
      						<Get>
      							<OnlineResource  xlink:href="%s/wms?SERVICE=WMS&REQUEST=GetCapabilities"/>
      						</Get>
      						<Post>
      							<OnlineResource xlink:href="%s/wms?SERVICE=WMS&amp;REQUEST=GetCapabilities"/>
      						</Post>
      					</HTTP>
      				</DCPType>
      			</GetCapabilities>
      			<GetMap>
      				<DCPType>
      					<HTTP>
      						<Get>
      							<OnlineResource xlink:href="%s/wms?SERVICE=WMS&amp;REQUEST=GetMap"/>
      						</Get>
      					</HTTP>
      				</DCPType>
      			</GetMap>
      			<GetFeatureInfo>
      				<DCPType>
      					<HTTP>
      						<Get>
      							<OnlineResource xlink:href="%s/wms?SERVICE=WMS&amp;REQUEST=GetFeatureInfo"/>
      						</Get>
      					</HTTP>
      				</DCPType>
      			</GetFeatureInfo>
      		</Request>
      		<Layer>
      			<Abstract>Direct access at %s/wms (internal only)</Abstract>
      		</Layer>
      	</Capability>
      </WMS_Capabilities>
      """,
            GEOSERVER, GEOSERVER, GEOSERVER, GEOSERVER, GEOSERVER, GEOSERVER);

    final RequestExecutorResponseImpl<byte[]> resp = response(inputXml);
    decorator.addBehavior(resp, capabilitiesPayload(SERVICE_URI));
    String result = bodyOf(resp);

    assertThat(result)
        .contains("xlink:href=\"" + PROXY_URL + "?SERVICE=WMS\"")
        .contains("xlink:href=\"" + PROXY_URL + "?SERVICE=WMS&amp;REQUEST=GetCapabilities\"")
        .contains("xlink:href=\"" + PROXY_URL + "?SERVICE=WMS&amp;REQUEST=GetMap\"")
        .contains("xlink:href=\"" + PROXY_URL + "?SERVICE=WMS&amp;REQUEST=GetFeatureInfo\"")
        .doesNotContain("\"" + GEOSERVER)
        .doesNotContain("'" + GEOSERVER)
        .contains("Direct access at " + GEOSERVER + "/wms (internal only)");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("servicePathCases")
  void servicePathBehavior(
      String name,
      List<String> servicePaths,
      String payloadUri,
      String xmlBody,
      String shouldContain) {
    properties.setServicePaths(servicePaths);
    final RequestExecutorResponseImpl<byte[]> resp = response(xmlBody);
    decorator.addBehavior(resp, capabilitiesPayload(payloadUri));
    assertThat(bodyOf(resp)).contains(shouldContain);
  }

  static Stream<Arguments> servicePathCases() {
    return Stream.of(
        Arguments.of(
            "custom suffix added to service-paths is recognized as an OGC endpoint",
            List.of("wms", "arcgis"),
            "https://some.domain.com/service/arcgis?SERVICE=WMS",
            onlineResourceWrap("https://some.domain.com/service/arcgis?SERVICE=WMS"),
            PROXY_URL + "?SERVICE=WMS"),
        Arguments.of(
            "suffix removed from service-paths is no longer matched as an OGC endpoint",
            List.of("wms"),
            GEOSERVER + "/wms",
            onlineResourceWrap(GEOSERVER + "/ows?SERVICE=WMS"),
            GEOSERVER + "/ows?SERVICE=WMS"));
  }
}
