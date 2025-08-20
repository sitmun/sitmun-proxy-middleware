package org.sitmun.proxy.middleware.protocols.wms;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.dto.HttpSecurityDto;
import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.protocols.http.HttpContext;

@Getter
@Setter
@JsonTypeName("OgcWmsPayload")
@NoArgsConstructor
public class WmsPayloadDto extends PayloadDto implements HttpContext {

  private String uri;
  private String method;
  private Map<String, String> parameters;
  private HttpSecurityDto security;
  private String body;

  @Builder
  public WmsPayloadDto(
      List<String> vary,
      String uri,
      String method,
      Map<String, String> parameters,
      HttpSecurityDto security,
      String body) {
    super(vary);
    this.uri = uri;
    this.method = method;
    this.parameters = parameters;
    this.security = security;
    this.body = body;
  }

  @Override
  public String describe() {
    return "OgcWmsPayloadDto{"
        + "vary="
        + getVary()
        + ", uri='"
        + uri
        + '\''
        + ", method='"
        + method
        + '\''
        + ", parameters="
        + parameters
        + ", security="
        + security
        + '}';
  }
}
