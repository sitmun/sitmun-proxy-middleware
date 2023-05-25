package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.dto.PayloadDto;
import org.sitmun.proxy.middleware.request.GlobalRequest;

public interface RequestDecorator {
	
	public boolean accept(PayloadDto payload);
	
	public void apply(GlobalRequest globalRequest, PayloadDto payload);
}
