package org.sitmun.proxy.middleware.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigProxyDto implements Serializable{

	private static final long serialVersionUID = -6818782288551485250L;

	private String type;
	
	private long exp;
	
	private List<String> vary;
	
	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
	@JsonSubTypes({@JsonSubTypes.Type(value = OgcWmsPayloadDto.class), @JsonSubTypes.Type(value = DatasourcePayloadDto.class)})
	private PayloadDto payload;

}