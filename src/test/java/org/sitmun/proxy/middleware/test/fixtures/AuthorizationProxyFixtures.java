package org.sitmun.proxy.middleware.test.fixtures;

import java.util.HashMap;
import org.sitmun.proxy.middleware.dto.HttpSecurityDto;
import org.sitmun.proxy.middleware.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;

public class AuthorizationProxyFixtures {

  public static WmsPayloadDto wmsService(boolean basicAuthentication) {
    return WmsPayloadDto.builder()
        .method("GET")
        .parameters(getWmsParameters())
        .security(
            basicAuthentication
                ? new HttpSecurityDto("basic", "http", "userServ", "passwordServ")
                : null)
        .uri("https://sitmun.diba.cat/arcgis/services/PUBLIC/DTE50/MapServer/WmsServer")
        .build();
  }

  public static WmsPayloadDto wmsServiceWithIPasHostname() {
    return WmsPayloadDto.builder()
        .method("GET")
        .parameters(getWmsParameters())
        .uri("http://154.58.18.33/arcgis/services/PUBLIC/DTE50/MapServer/WmsServer")
        .build();
  }

  public static WmsPayloadDto wmsServiceWithURIWithParameters() {
    return WmsPayloadDto.builder()
        .method("GET")
        .parameters(getWmsParameters())
        .uri(
            "https://sitmun.diba.cat/arcgis/services/PUBLIC/DTE50/MapServer/WmsServer?service=WMS&")
        .build();
  }

  public static WmsPayloadDto wfsService(boolean filtered) {
    WmsPayloadDto payload = new WmsPayloadDto();
    payload.setMethod("GET");
    payload.setParameters(getWfsParameters(filtered));
    payload.setSecurity(null);
    payload.setUri(
        "https://www.juntadeandalucia.es/institutodeestadisticaycartografia/geoserver-ieca/grid/wfs");
    payload.setVary(null);
    return payload;
  }

  private static HashMap<String, String> getWmsParameters() {
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("REQUEST", "GetMap");
    parameters.put("VERSION", "1.3.0");
    parameters.put("SERVICE", "WMS");
    parameters.put("LAYERS", "DTE50_MUN,DTE50_PROV");
    parameters.put(
        "BBOX", "2.1358108520507812,41.37616450732182,2.1797561645507812,41.39986165460519");
    parameters.put("CRS", "EPSG:4326");
    parameters.put("STYLES", "");
    parameters.put("FORMAT", "image/png");
    parameters.put("TRANSPARENT", "true");
    parameters.put("width", "256");
    parameters.put("height", "256");
    return parameters;
  }

  private static HashMap<String, String> getWfsParameters(boolean filtered) {
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("REQUEST", "GetFeature");
    parameters.put("VERSION", "2.0.0");
    parameters.put("SERVICE", "WFS");
    parameters.put("outputformat", "application/json");
    parameters.put("typename", "grid:gridp_250");

    if (filtered) {
      parameters.put("CQL_FILTER", "tr_05=5");
    } else {
      parameters.put(
          "BBOX", "243818.6189194798,4133819.057198299,255162.31367381044,4141774.181994748");
    }

    return parameters;
  }

  private static String getSql(boolean filtered) {
    String sql = "SELECT table_name, table_schema FROM INFORMATION_SCHEMA.TABLES";
    if (filtered) {
      sql += " WHERE table_name like 'TABLE%'";
    }
    return sql;
  }

  public static JdbcPayloadDto inMemoryH2Database(boolean filtered) {
    return JdbcPayloadDto.builder()
        .driver("org.h2.Driver")
        .uri("jdbc:h2:mem:testdb")
        .user("admin")
        .password("admin")
        .sql(getSql(filtered))
        .build();
  }
}
