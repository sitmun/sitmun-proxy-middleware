package org.sitmun.proxy.middleware.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigProxyDto {

  private String type;

  private long exp;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({@JsonSubTypes.Type(value = OgcWmsPayloadDto.class), @JsonSubTypes.Type(value = DatasourcePayloadDto.class)})
  private PayloadDto payload;

}
