package org.sitmun.proxy.middleware.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.decorator.HttpContext;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonTypeName("OgcWmsPayload")
@NoArgsConstructor
public class OgcWmsPayloadDto extends PayloadDto implements HttpContext {

  private String uri;
  private String method;
  private Map<String, String> parameters;
  private HttpSecurityDto security;

  @Builder
  public OgcWmsPayloadDto(List<String> vary, String uri, String method, Map<String, String> parameters, HttpSecurityDto security) {
    super(vary);
    this.uri = uri;
    this.method = method;
    this.parameters = parameters;
    this.security = security;
  }

  @Override
  public String describe() {
    return "OgcWmsPayloadDto{" +
      "vary=" + getVary() +
      ", uri='" + uri + '\'' +
      ", method='" + method + '\'' +
      ", parameters=" + parameters +
      ", security=" + security +
      '}';
  }
}
