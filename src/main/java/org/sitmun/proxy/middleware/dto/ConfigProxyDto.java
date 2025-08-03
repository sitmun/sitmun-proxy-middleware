package org.sitmun.proxy.middleware.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import org.sitmun.proxy.middleware.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigProxyDto {

  private String type;

  private long exp;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = WmsPayloadDto.class),
    @JsonSubTypes.Type(value = JdbcPayloadDto.class)
  })
  private PayloadDto payload;
}
