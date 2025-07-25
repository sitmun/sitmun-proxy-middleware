package org.sitmun.proxy.middleware.process;

import org.sitmun.proxy.middleware.decorator.MbtilesContext;
import org.sitmun.proxy.middleware.dto.MBTilesEstimateDto;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.scope.context.StepContext;

public interface TileProcess {
    
    public MBTilesEstimateDto estimateSize(MbtilesContext mbtilesContext) throws Exception;

    public void process(MbtilesContext mbtilesContext, StepContext stepContext) throws JobExecutionException;
}
