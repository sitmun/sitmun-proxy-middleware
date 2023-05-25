package org.sitmun.proxy.middleware.decorator.response;

import org.springframework.http.ResponseEntity;

public interface ResponseDecorator {
	
	public boolean accept(ResponseEntity<?> response);
	
	public void apply(ResponseEntity<?> response);
}
