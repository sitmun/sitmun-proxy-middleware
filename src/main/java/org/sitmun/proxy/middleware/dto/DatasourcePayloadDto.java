package org.sitmun.proxy.middleware.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("DatasourcePayload")
public class DatasourcePayloadDto extends PayloadDto {

	private String uri;
	
	private String user;
	
	private String password;
	
	private String driver;
	
	private String sql;
}
