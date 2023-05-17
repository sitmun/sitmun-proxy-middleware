package org.sitmun.proxy.middleware.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO implements Serializable {
	
	private int status;
	private String error;
	private String message;
	private String path;
	private Date timestamp;
}
