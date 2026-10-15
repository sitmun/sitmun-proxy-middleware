package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
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
  void shouldAcceptWhenContextIsHttpContextWithPostMethodAndNonNullBody() {
    when(httpContext.getMethod()).thenReturn("POST");
    when(httpContext.getBody()).thenReturn("<request>data</request>");

    boolean result = decorator.accept(requestExecutor, httpContext);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should not accept when context is payload with GET method")
  void shouldNotAcceptWhenContextIsHttpContextWithGetMethod() {
    when(httpContext.getMethod()).thenReturn("GET");

    boolean result = decorator.accept(requestExecutor, httpContext);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is payload with POST method but null body")
  void shouldNotAcceptWhenContextIsHttpContextWithPostMethodButNullBody() {
    when(httpContext.getMethod()).thenReturn("POST");
    when(httpContext.getBody()).thenReturn(null);

    boolean result = decorator.accept(requestExecutor, httpContext);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should accept when context is payload with POST method and empty body")
  void shouldAcceptWhenContextIsHttpContextWithPostMethodAndEmptyBody() {
    when(httpContext.getMethod()).thenReturn("POST");
    when(httpContext.getBody()).thenReturn("");

    boolean result = decorator.accept(requestExecutor, httpContext);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should handle XML body with complex structure")
  void shouldHandleXmlBodyWithComplexStructure() {
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

    decorator.addBehavior(requestExecutor, httpContext);

    assertThat(requestExecutor.getHeader("Content-Type")).isEqualTo("application/xml");
    assertThat(requestExecutor).extracting("body").isEqualTo(body);
  }

  @Test
  @DisplayName("Should reject a lowercase post method")
  void shouldRejectLowercasePostMethod() {
    when(httpContext.getMethod()).thenReturn("post");
    lenient().when(httpContext.getBody()).thenReturn("<request>data</request>");

    boolean result = decorator.accept(requestExecutor, httpContext);

    assertThat(result).isFalse();
  }
}
