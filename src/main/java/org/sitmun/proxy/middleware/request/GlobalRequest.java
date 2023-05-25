package org.sitmun.proxy.middleware.request;

import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class GlobalRequest {

  private CustomHttpRequest customHttpRequest;

  private JdbcRequest jdbcRequest;

  public void setMapServiceRequest() {
    this.customHttpRequest = new CustomHttpRequest();
  }

  public void setDatabaseRequest() {
    this.jdbcRequest = new JdbcRequest();
  }

  public ResponseEntity<?> execute() {
    if (customHttpRequest != null) {
      return executeHttp();
    } else if (jdbcRequest != null) {
      return executeJdbc();
    }
    return null;
  }

  private ResponseEntity<?> executeHttp() {
    OkHttpClient httpClient = new OkHttpClient();
    Request httpRequest = customHttpRequest.getRequestBuilder().build();
    Response response = null;
    try {
      response = httpClient.newCall(httpRequest).execute();
      return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(response.header("content-type")))
        .body(response.body().bytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private ResponseEntity<?> executeJdbc() {

    List<Map<String, Object>> result = new ArrayList<>();
    try(Connection connection = jdbcRequest.getConnection()) {
      executeStatement(connection, result);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return ResponseEntity.ok().body(result);
  }

  private void executeStatement(Connection connection, List<Map<String, Object>> result) {
    try(Statement stmt = connection.createStatement()) {
      retrieveResultSetMetadata(stmt, result);
    }  catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void retrieveResultSetMetadata(Statement stmt, List<Map<String, Object>> result) {
    try(ResultSet resultSet = stmt.executeQuery(jdbcRequest.getSql())) {
      ResultSetMetaData rsmd = resultSet.getMetaData();
      while (resultSet.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
          Object value = resultSet.getObject(i);
          row.put(rsmd.getColumnLabel(i), value);
        }
        result.add(row);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
