package org.sitmun.proxy.middleware.jobs;

import java.util.List;
import java.util.Map;

import org.sitmun.proxy.middleware.decorator.MbtilesContext;
import org.sitmun.proxy.middleware.decorator.MbtilesDecorator;
import org.sitmun.proxy.middleware.dto.MapServiceDto;
import org.sitmun.proxy.middleware.dto.TileServiceDto;
import org.sitmun.proxy.middleware.request.TileRequestDto;
import org.sitmun.proxy.middleware.service.MBTilesService;
import org.sitmun.proxy.middleware.utils.Constants;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MBTilesTask {

    @Autowired
    private MBTilesService mbTilesService;

    @Autowired
    private List<MbtilesDecorator> mbtilesDecorators;

    public void execute(StepContext context) throws JobExecutionException{
        Map<String, Object> jobParameters = context.getJobParameters();
        String outputPath = (String)jobParameters.get("outputPath");
        TileRequestDto tileRequest = null;
        try {
            tileRequest = new ObjectMapper().readValue((String)jobParameters.get("tileRequest"), TileRequestDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            //throw new JobExecutionException("Error al mapear la petición");
        }
        for (MapServiceDto ms : tileRequest.getMapServices()) {
            TileServiceDto tileService = new TileServiceDto(
                ms.getUrl(), ms.getLayers(), ms.getType(),
                tileRequest.getMinLat(), tileRequest.getMinLon(),
                tileRequest.getMaxLat(), tileRequest.getMaxLon(),
                tileRequest.getMinZoom(), tileRequest.getMaxZoom(),
                tileRequest.getSrs(), Constants.MBTilesSrs
            );
            processTile(tileService, outputPath);
        }
    }

    private void processTile(TileServiceDto service, String outputPath) throws JobExecutionException {
        MbtilesContext context = new MbtilesContext(service, outputPath);
        try {
            for (MbtilesDecorator md : mbtilesDecorators) {
                md.apply(mbTilesService, context);
            }
        } catch (Exception e) {
            throw new JobExecutionException(e.getMessage());
        }
    }
}
