package org.sitmun.proxy.middleware.protocols.jdbc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.dto.PayloadDto;

@Getter
@Setter
@JsonTypeName("DatasourcePayload")
@NoArgsConstructor
public class JdbcPayloadDto extends PayloadDto implements JdbcContext {

  private String uri;
  private String user;
  private String password;
  private String driver;
  private String sql;

  @Builder
  public JdbcPayloadDto(
      List<String> vary, String uri, String user, String password, String driver, String sql) {
    super(vary);
    this.uri = uri;
    this.user = user;
    this.password = password;
    this.driver = driver;
    this.sql = sql;
  }

  @Override
  public String describe() {
    return "DatasourcePayloadDto{"
        + "vary="
        + getVary()
        + ", uri='"
        + uri
        + '\''
        + ", user='"
        + user
        + '\''
        + ", password='****'"
        + ", driver='"
        + driver
        + '\''
        + ", sql='"
        + sql
        + '\''
        + '}';
  }
}
