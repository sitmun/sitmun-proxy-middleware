package org.sitmun.proxy.middleware.controllers;

import org.sitmun.proxy.middleware.request.TileRequestDto;
import org.sitmun.proxy.middleware.response.Response;
import org.sitmun.proxy.middleware.service.ProxyMiddlewareService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/proxy")
public class ProxyMiddlewareController {

  private final ProxyMiddlewareService proxyMiddlewareService;

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job mbTilesJob;

  public ProxyMiddlewareController(ProxyMiddlewareService proxyMiddlewareService,
    JobLauncher jobLauncher, JobExplorer jobExplorer, Job mbTilesJob) {
    this.proxyMiddlewareService = proxyMiddlewareService;
    this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.mbTilesJob = mbTilesJob;
  }

  @GetMapping("/{appId}/{terId}/{type}/{typeId}")
  public ResponseEntity<?> getService(@PathVariable("appId") Integer appId,
                                      @PathVariable("terId") Integer terId,
                                      @PathVariable("type") String type,
                                      @PathVariable("typeId") Integer typeId,
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                      @RequestParam(required = false) Map<String, String> params,
                                      HttpServletRequest request) {
    String token = authorization != null ? authorization.substring(7) : null;
    String url = request.getRequestURL().toString();
    return proxyMiddlewareService.doRequest(appId, terId, type, typeId, token, params, url);
  }

  @PostMapping(value = "/mbtiles", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<?> startMBTilesJob(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @RequestBody TileRequestDto tileRequest) throws Exception {
    String token = authorization != null ? authorization.substring(7) : null;
    String uuid = UUID.randomUUID().toString();
    String outputPath = File.createTempFile(uuid, ".mbtiles").getAbsolutePath();

    Map<String, JobParameter> params = new HashMap<>();
    params.put("outputPath", new JobParameter(outputPath));
    params.put("tileRequest", new JobParameter(new ObjectMapper().writeValueAsString(tileRequest)));
    params.put("timestamp", new JobParameter(System.currentTimeMillis()));

    JobExecution jobExecution = jobLauncher.run(mbTilesJob, new JobParameters(params));

    Response<String> response = new Response<>("", 200, MediaType.TEXT_PLAIN_VALUE, jobExecution.getId().toString());
    return response.asResponseEntity();
  }

  @GetMapping("/mbtiles/{jobId}")
  public ResponseEntity<?> getMBTilesJobStatus(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                               @PathVariable Long jobId) {
    String token = authorization != null ? authorization.substring(7) : null;
    int code = 200;
    String status = "";
    try {
      JobExecution jobExecution = jobExplorer.getJobExecution(jobId);

      if (jobExecution == null) {        
        code = 404;
        status = "NOT FOUND";
      } else {
        status = jobExecution.getStatus().toString();
      }
      Response<String> response = new Response<>("", code, MediaType.TEXT_PLAIN_VALUE, status);
      return response.asResponseEntity();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
    }
  }

  @GetMapping("/mbtiles/{jobId}/file")
  public ResponseEntity<Resource> downloadFile(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                               @PathVariable Long jobId) throws IOException {
    String token = authorization != null ? authorization.substring(7) : null;
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
}
