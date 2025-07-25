package org.sitmun.proxy.middleware.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sitmun.proxy.middleware.dto.MBTilesProgressDto;
import org.springframework.stereotype.Service;

@Service
public class MBTilesProgressService {

    private final Map<Long, MBTilesProgressDto> jobProgress = new ConcurrentHashMap<>();
    
    public void updateJobProgress(long jobId, long totalTiles, long processedTiles) {
        MBTilesProgressDto progress = jobProgress.get(jobId);
        if (progress == null) {
            progress = new MBTilesProgressDto(totalTiles, processedTiles);
            jobProgress.put(jobId, progress);
        } else {
            progress.setProcessedTiles(processedTiles);
        }
    }

    public MBTilesProgressDto getJobProgress(long jobId) {
        return jobProgress.get(jobId);
    }

    public void clearJobProgress(long jobId) {
        jobProgress.remove(jobId);
    }
}
