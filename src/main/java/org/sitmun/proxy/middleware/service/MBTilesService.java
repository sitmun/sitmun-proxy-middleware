package org.sitmun.proxy.middleware.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sitmun.proxy.middleware.decorator.MbtilesContext;
import org.sitmun.proxy.middleware.decorator.MbtilesEstimationDecorator;
import org.sitmun.proxy.middleware.dto.MBTilesEstimateDto;
import org.sitmun.proxy.middleware.dto.MBTilesProgressDto;
import org.sitmun.proxy.middleware.dto.MapServiceDto;
import org.sitmun.proxy.middleware.dto.TileServiceDto;
import org.sitmun.proxy.middleware.request.TileRequestDto;
import org.sitmun.proxy.middleware.utils.Constants;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MBTilesService {

    private final JobLauncher jobLauncher;

    private final JobExplorer jobExplorer;

    private final Job mbTilesJob;

    private final MBTilesProgressService mbTilesProgressService;

    @Autowired
    private List<MbtilesEstimationDecorator> mbtilesDecorators;

    public MBTilesService(JobLauncher jobLauncher, JobExplorer jobExplorer, Job mbTilesJob,
                         MBTilesProgressService mbTilesProgressService) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.mbTilesJob = mbTilesJob;
        this.mbTilesProgressService = mbTilesProgressService;
    }

    public Long startJob(TileRequestDto tileRequest) throws Exception {
        String uuid = UUID.randomUUID().toString();
        String outputPath = File.createTempFile(uuid, ".mbtiles").getAbsolutePath();

        Map<String, JobParameter> params = new HashMap<>();
        params.put("outputPath", new JobParameter(outputPath));
        params.put("tileRequest", new JobParameter(new ObjectMapper().writeValueAsString(tileRequest)));
        params.put("timestamp", new JobParameter(System.currentTimeMillis()));
        JobExecution jobExecution = jobLauncher.run(mbTilesJob, new JobParameters(params));

        return jobExecution.getId();
    }

    public ResponseEntity<?> getJobStatus(long jobId) {
        Map<String, Object> statusMap = new HashMap<>();
        int code = 200;
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(jobId);

            if (jobExecution == null) {        
                code = 404;
                statusMap.put("status", "NOT FOUND");
            } else {
                BatchStatus batchStatus = jobExecution.getStatus();
                if (BatchStatus.FAILED.equals(batchStatus)) {
                    code = 500;
                    statusMap.put("status", "FAILED");
                } else {
                    statusMap.put("status", batchStatus.toString());
                }
                if (BatchStatus.COMPLETED.equals(batchStatus) || BatchStatus.FAILED.equals(batchStatus)) {
                    mbTilesProgressService.clearJobProgress(jobId);
                }
                MBTilesProgressDto progressDto = mbTilesProgressService.getJobProgress(jobId);
                if (progressDto != null) {
                    statusMap.put("processedTiles", progressDto.getProcessedTiles());
                    statusMap.put("totalTiles", progressDto.getTotalTiles());
                }                    
            }
            return ResponseEntity.status(code)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(statusMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    public ResponseEntity<Resource> getMBTilesFile(long jobId) throws IOException{
        JobExecution jobExecution = jobExplorer.getJobExecution(jobId);

        if (jobExecution == null || !BatchStatus.COMPLETED.equals(jobExecution.getStatus())) {
            return ResponseEntity.notFound().build();
        }

        String outputPath = jobExecution.getJobParameters().getString("outputPath");
        Path path = Paths.get(outputPath);

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String[] pathParts = outputPath.split("/");
        String fileName = pathParts[pathParts.length - 1];

        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentLength(Files.size(path))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    public ResponseEntity<MBTilesEstimateDto> estimateSize(TileRequestDto tileRequest) {
        MBTilesEstimateDto estimation = new MBTilesEstimateDto(0, 0, 0);
        for (MapServiceDto ms : tileRequest.getMapServices()) {
            TileServiceDto tileService = new TileServiceDto(
                ms.getUrl(), ms.getLayers(), ms.getType(),
                tileRequest.getMinLat(), tileRequest.getMinLon(),
                tileRequest.getMaxLat(), tileRequest.getMaxLon(),
                tileRequest.getMinZoom(), tileRequest.getMaxZoom(),
                tileRequest.getSrs(), Constants.MBTilesSrs
            );
            MbtilesContext context = new MbtilesContext(tileService, null);
            try {
                for (MbtilesEstimationDecorator md : mbtilesDecorators) {
                    Object result = md.apply(null, context);
                    if (result != null) {
                        sumMBtilesEstimations(estimation, (MBTilesEstimateDto)result);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok(estimation);
    }

    private void sumMBtilesEstimations(MBTilesEstimateDto total, MBTilesEstimateDto newSize) {
        double newEstimateTileSize = (total.getEstimatedTileSizeKb() * total.getTileCount() + newSize.getEstimatedTileSizeKb() * newSize.getTileCount()) / (total.getTileCount() + newSize.getTileCount());
        total.setTileCount(total.getTileCount() + newSize.getTileCount());
        total.setEstimatedTileSizeKb(newEstimateTileSize);
        total.setEstimatedMbtilesSizeMb(total.getEstimatedMbtilesSizeMb() + newSize.getEstimatedMbtilesSizeMb());
    }

}

