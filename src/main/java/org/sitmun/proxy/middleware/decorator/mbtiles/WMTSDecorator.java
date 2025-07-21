package org.sitmun.proxy.middleware.decorator.mbtiles;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.MbtilesContext;
import org.sitmun.proxy.middleware.decorator.MbtilesDecorator;
import org.sitmun.proxy.middleware.service.MBTilesService;
import org.sitmun.proxy.middleware.utils.Constants;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class WMTSDecorator implements MbtilesDecorator{

    @Override
    public boolean accept(Object target, Context context) {
        boolean accept = false;
        if (context instanceof MbtilesContext) {
            String serviceType = ((MbtilesContext)context).getService().getType();
            accept = Constants.WMTSType.equals(serviceType);
        }
        return accept;
    }

    @Override
    public void addBehavior(Object target, Context context) throws JobExecutionException{
        MBTilesService mbTilesService = (MBTilesService) target;
        mbTilesService.processWMTSTile((MbtilesContext)context);
    }
    
}
