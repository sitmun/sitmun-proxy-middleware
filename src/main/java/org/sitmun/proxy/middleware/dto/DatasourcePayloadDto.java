package org.sitmun.proxy.middleware.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.decorator.JdbcContext;

import java.util.List;

@Getter
@Setter
@JsonTypeName("DatasourcePayload")
@NoArgsConstructor
public class DatasourcePayloadDto extends PayloadDto implements JdbcContext {

  private String uri;
  private String user;
  private String password;
  private String driver;
  private String sql;


  @Builder
  public DatasourcePayloadDto(List<String> vary, String uri, String user, String password, String driver, String sql) {
    super(vary);
    this.uri = uri;
    this.user = user;
    this.password = password;
    this.driver = driver;
    this.sql = sql;
  }

}
