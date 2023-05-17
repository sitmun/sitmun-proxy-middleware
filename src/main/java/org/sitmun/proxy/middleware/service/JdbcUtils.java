package org.sitmun.proxy.middleware.service;

import java.util.List;
import java.util.Map;

import org.sitmun.proxy.middleware.dto.DatasourcePayloadDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

@Component
public class JdbcUtils {

	public List<Map<String, Object>> doQuery(DatasourcePayloadDto datasourcePayload) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(createDatasource(datasourcePayload));
		List<Map<String, Object>> results = jdbcTemplate.queryForList(datasourcePayload.getSql());
		closeTemplateConnection(jdbcTemplate);
		return results;
	}
	
	private HikariDataSource createDatasource(DatasourcePayloadDto datasourcePayload) {
		HikariDataSource datasource = new HikariDataSource();
		datasource.setDriverClassName(datasourcePayload.getDriver());
		datasource.setPassword(datasourcePayload.getPassword());
		datasource.setReadOnly(true);
		datasource.setUsername(datasourcePayload.getUser());
		datasource.setJdbcUrl(datasourcePayload.getUri());

		return datasource;
	}
	
	private void closeTemplateConnection(JdbcTemplate jdbcTemplate) {
		HikariDataSource datasource = (HikariDataSource)jdbcTemplate.getDataSource();
		datasource.close();
	}
}
