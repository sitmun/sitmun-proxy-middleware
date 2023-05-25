package org.sitmun.proxy.middleware.request;

import java.sql.Connection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class JdbcRequest {

	private Connection connection;
	
	private String sql;
}
