package org.sitmun.proxy.middleware.request;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
		if(customHttpRequest != null) {
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
		Connection connection = jdbcRequest.getConnection();
		Statement statement = null;
		ResultSet resultSet = null;
		List<Map<String, Object>> result = new ArrayList();
		try {
			if (connection != null) {
				statement = connection.createStatement();
				resultSet = statement.executeQuery(jdbcRequest.getSql());
				ResultSetMetaData rsmd = resultSet.getMetaData();	
				while (resultSet.next()) {
					Map<String, Object> row = new HashMap();
					for(int i = 1; i <= rsmd.getColumnCount(); i++){
						Object value = resultSet.getObject(i);
						row.put(rsmd.getColumnLabel(i), value);
					}
					result.add(row);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return ResponseEntity.ok().body(result);
	}
}
