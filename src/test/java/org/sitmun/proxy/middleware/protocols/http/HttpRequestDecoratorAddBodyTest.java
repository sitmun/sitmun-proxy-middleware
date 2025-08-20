package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestDecoratorAddBody tests")
class HttpRequestDecoratorAddBodyTest {

  private HttpRequestDecoratorAddBody decorator;
  private HttpRequestExecutor requestExecutor;

  @Mock private HttpContext httpContext;

  @BeforeEach
  void setUp() {
    decorator = new HttpRequestDecoratorAddBody();
    requestExecutor = new HttpRequestExecutor("http://test.com", null);
  }

  @Test
  @DisplayName("Should accept when context is payload with POST method and non-null body")
  void shouldAcceptWhenContextIsWmsPayloadDtoWithPostMethodAndNonNullBody() {
    // Given
    when(httpContext.getMethod()).thenReturn("POST");
    when(httpContext.getBody()).thenReturn("<request>data</request>");

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should not accept when context is payload with GET method")
  void shouldNotAcceptWhenContextIsWmsPayloadDtoWithGetMethod() {
    // Given
    when(httpContext.getMethod()).thenReturn("GET");

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is payload with POST method but null body")
  void shouldNotAcceptWhenContextIsWmsPayloadDtoWithPostMethodButNullBody() {
    // Given
    when(httpContext.getMethod()).thenReturn("POST");
    when(httpContext.getBody()).thenReturn(null);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should accept when context is payload with POST method and empty body")
  void shouldAcceptWhenContextIsWmsPayloadDtoWithPostMethodAndEmptyBody() {
    // Given
    when(httpContext.getMethod()).thenReturn("POST");
    when(httpContext.getBody()).thenReturn("");

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isTrue(); // Implementation only checks for != null, not empty
  }

  @Test
  @DisplayName("Should handle XML body with complex structure")
  void shouldHandleXmlBodyWithComplexStructure() {
    // Given
    String body =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <GetMap xmlns="http://www.opengis.net/sld">
          <StyledLayerDescriptor version="1.0.0">
            <NamedLayer>
              <Name>test-layer</Name>
              <UserStyle>
                <Title>Test Style</Title>
                <FeatureTypeStyle>
                  <Rule>
                    <PolygonSymbolizer>
                      <Fill>
                        <CssParameter name="fill">#ff0000</CssParameter>
                      </Fill>
                    </PolygonSymbolizer>
                  </Rule>
                </FeatureTypeStyle>
              </UserStyle>
            </NamedLayer>
          </StyledLayerDescriptor>
        </GetMap>""";

    when(httpContext.getBody()).thenReturn(body);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    String description = requestExecutor.describe();
    assertThat(description).contains("Content-Type=application/xml");
  }

  @Test
  @DisplayName("Should handle XML body with CDATA sections")
  void shouldHandleXmlBodyWithCdataSections() {
    // Given
    String body =
        "<request><data><![CDATA[<html><body>This is HTML content</body></html>]]></data></request>";
    when(httpContext.getBody()).thenReturn(body);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    String description = requestExecutor.describe();
    assertThat(description).contains("Content-Type=application/xml");
  }

  @Test
  @DisplayName("Should handle XML body with namespaces")
  void shouldHandleXmlBodyWithNamespaces() {
    // Given
    String body =
        """
            <wms:GetMap xmlns:wms="http://www.opengis.net/wms" xmlns:ogc="http://www.opengis.net/ogc">
              <wms:Layer>test-layer</wms:Layer>
              <ogc:Filter>
                <ogc:PropertyIsEqualTo>
                  <ogc:PropertyName>status</ogc:PropertyName>
                  <ogc:Literal>active</ogc:Literal>
                </ogc:PropertyIsEqualTo>
              </ogc:Filter>
            </wms:GetMap>""";
    when(httpContext.getBody()).thenReturn(body);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    String description = requestExecutor.describe();
    assertThat(description).contains("Content-Type=application/xml");
  }

  @Test
  @DisplayName("Should handle case-insensitive method comparison")
  void shouldHandleCaseInsensitiveMethodComparison() {
    // Given
    when(httpContext.getMethod()).thenReturn("post"); // lowercase

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse(); // Should be false because it's case-sensitive
  }
}
