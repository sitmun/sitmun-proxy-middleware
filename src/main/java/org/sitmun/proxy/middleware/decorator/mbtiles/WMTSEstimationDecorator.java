package org.sitmun.proxy.middleware.decorator.mbtiles;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.MbtilesContext;
import org.sitmun.proxy.middleware.decorator.MbtilesEstimationDecorator;
import org.sitmun.proxy.middleware.dto.MBTilesEstimateDto;
import org.sitmun.proxy.middleware.process.WMTSTileProcess;
import org.sitmun.proxy.middleware.utils.Constants;
import org.springframework.stereotype.Component;

@Component
public class WMTSEstimationDecorator implements MbtilesEstimationDecorator{

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
    public MBTilesEstimateDto addBehavior(Object target, Context context) throws Exception{
        WMTSTileProcess tileProcess = new WMTSTileProcess();
        return tileProcess.estimateSize((MbtilesContext)context);
    }
    
}
