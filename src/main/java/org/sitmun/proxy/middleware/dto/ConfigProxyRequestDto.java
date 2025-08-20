package org.sitmun.proxy.middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigProxyRequestDto {

  @JsonProperty("appId")
  private int appId;

  @JsonProperty("terId")
  private int terId;

  @JsonProperty("type")
  private String type;

  @JsonProperty("typeId")
  private int typeId;

  @JsonProperty("method")
  private String method;

  @JsonProperty("parameters")
  private Map<String, String> parameters;

  @JsonProperty("requestBody")
  private String requestBody;

  @JsonProperty("id_token")
  private String token;
}
