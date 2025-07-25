package org.sitmun.proxy.middleware.process;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.sitmun.proxy.middleware.decorator.MbtilesContext;
import org.sitmun.proxy.middleware.dto.Capabilities;
import org.sitmun.proxy.middleware.dto.LayerCapabilities;
import org.sitmun.proxy.middleware.dto.MBTilesEstimateDto;
import org.sitmun.proxy.middleware.dto.TileCoordinate;
import org.sitmun.proxy.middleware.dto.TileMatrix;
import org.sitmun.proxy.middleware.dto.TileServiceDto;
import org.sitmun.proxy.middleware.service.MBTilesProgressService;
import org.sitmun.proxy.middleware.utils.CapabilitiesUtils;
import org.sitmun.proxy.middleware.utils.Constants;
import org.sitmun.proxy.middleware.utils.CustomMBTilesReader;
import org.sitmun.proxy.middleware.utils.CustomMBTilesWriter;
import org.sitmun.proxy.middleware.utils.Proj4Utils;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.poole.geo.mbtiles4j.Tile;
import ch.poole.geo.mbtiles4j.model.MetadataBounds;
import ch.poole.geo.mbtiles4j.model.MetadataEntry;
import ch.poole.geo.mbtiles4j.model.MetadataEntry.TileMimeType;
import ch.poole.geo.mbtiles4j.model.MetadataEntry.TileSetType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WMTSTileProcess implements TileProcess {

    private static final String TEMPLATE = "%s?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&LAYER=%s&TILEMATRIXSET=%s&TILEMATRIX=%s&TILEROW=%d&TILECOL=%d&FORMAT=image/png";

    @Autowired
    private MBTilesProgressService mbTilesProgressService;

    @Override
    public MBTilesEstimateDto estimateSize(MbtilesContext mbtilesContext) throws Exception {
        TileServiceDto service = mbtilesContext.getService();
        Capabilities capabilities = CapabilitiesUtils.parseWMTSCapabilities(service);
        double[] bounds = {service.getMinLon(), service.getMinLat(), service.getMaxLon(), service.getMaxLat()};
        int tileCount = 0;

        if (!service.getSrs().equals(Constants.MBTilesSrs)) {
            bounds = Proj4Utils.transformExtent(bounds, service.getSrs(), Constants.MBTilesSrs);
        }
        double estimateSize = 0;
        for (LayerCapabilities lc : capabilities.getLayers()) {
            log.info("Estimating size for layer: " + lc.getLayerIdentifier());
            List<TileCoordinate> coordinates = calculateCoordinates(service, lc, bounds);
            estimateSize += getAvgTileSize(service, lc.getLayerIdentifier(), coordinates) / 1024;
            tileCount += coordinates.size();
        }
        log.info("Estimate tile size: " + (estimateSize / tileCount) + " KB per tile");
        return new MBTilesEstimateDto(tileCount, estimateSize / tileCount, estimateSize / 1024);
    }

    @Override
    public void process(MbtilesContext mbtilesContext, StepContext stepContext) throws JobExecutionException {
        try {
            TileServiceDto service = mbtilesContext.getService();
            String outputPath = mbtilesContext.getOutputPath();
            Capabilities capabilities = CapabilitiesUtils.parseWMTSCapabilities(service);
            File outputFile = new File(outputPath);
            CustomMBTilesWriter writer = new CustomMBTilesWriter(outputFile);
            CustomMBTilesReader reader = new CustomMBTilesReader(outputFile);
            double[] bounds = {service.getMinLon(), service.getMinLat(), service.getMaxLon(), service.getMaxLat()};

            if (!service.getSrs().equals(Constants.MBTilesSrs)) {
                bounds = Proj4Utils.transformExtent(bounds, service.getSrs(), Constants.MBTilesSrs);
            }
            long procesados = 0;
            long total = 0;
            Map<LayerCapabilities, List<TileCoordinate>> layersCoordinates = new HashMap<>();
            for (LayerCapabilities lc : capabilities.getLayers()) {
                List<TileCoordinate> coordinates = calculateCoordinates(service, lc, bounds);
                layersCoordinates.put(lc, coordinates);
                total += coordinates.size();
            }
            long jobId = stepContext.getJobInstanceId();
            mbTilesProgressService.updateJobProgress(jobId, total, procesados);
            for (Map.Entry<LayerCapabilities, List<TileCoordinate>> entry : layersCoordinates.entrySet()) {
                LayerCapabilities lc = entry.getKey();
                List<TileCoordinate> coordinates = entry.getValue();
                for (TileCoordinate coord : coordinates) {
                    BufferedImage tile = getTileImage(service, lc.getLayerIdentifier(), coord);
                    if (tile != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int zoom = parseZoom(coord.getZ());
                        int yTMS = invertTileRow(coord.getY(), zoom);
                        BufferedImage saveTile = checkTileImage(reader, zoom, coord.getX(), yTMS);
                        if (saveTile == null) {
                            ImageIO.write(tile, "png", baos);
                            writer.addTile(baos.toByteArray(), zoom, coord.getX(), yTMS);
                        } else {
                            BufferedImage combinedTile = writer.combineTiles(saveTile, tile);
                            ImageIO.write(combinedTile, "png", baos);
                            writer.updateTile(zoom, coord.getX(), yTMS, baos.toByteArray());
                        }
                        log.info("Writing tile " + lc.getLayerIdentifier() + " Z: " + zoom + ", X: " + coord.getX() + ", Y: " + coord.getY());
                        log.info("Job: " + jobId + ", processed tiles: " + (procesados + 1) + " of " + total);
                    }
                    procesados++;
                    if (procesados % 50 == 0) {
                        mbTilesProgressService.updateJobProgress(jobId, total, procesados);
                    }
                }
            }
            bounds = calculateBounds(service, capabilities.getLayers());
            MetadataBounds metBounds = new MetadataBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
            writer.addMetadataEntry(new MetadataEntry(service.getLayers().get(0), TileSetType.BASE_LAYER, "1.0",
                "Layer generated by Sitmun proxy middleware", TileMimeType.PNG, metBounds));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JobExecutionException("Job failed: " + e.getMessage());
        }
    }

    private BufferedImage checkTileImage(CustomMBTilesReader reader, int z, int x, int y) {
        try {
            Tile tile = reader.getTile(z, x, y);
            if (tile != null && tile.getData() != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(tile.getData().readAllBytes());
                return ImageIO.read(bais);
            }
            return null;
        } catch (Exception e) {
            log.error("Error reading tile Z: " + z + ", X: " + x + ", Y: " + y + " - " + e.getMessage());
            return null;
        }
    }

    private List<TileCoordinate> calculateCoordinates(TileServiceDto service, LayerCapabilities layerCapabilities, double[] bounds) {
        List<TileCoordinate> list = new ArrayList<>();
        /*double[] layerBounds = {layerCapabilities.getMinLon(), layerCapabilities.getMinLat(),
            layerCapabilities.getMaxLon(), layerCapabilities.getMaxLat()};
        layerBounds = Proj4Utils.transformExtent(layerBounds, Constants.capabilitiesExtentSrs, Constants.MBTilesSrs);*/
        for (int zoom = service.getMinZoom(); zoom <= service.getMaxZoom(); zoom++) {
            String identifier = service.getMatrixSet() + ":" + zoom;
            TileMatrix tileMatrix = calculateTileMatrixByExtent(layerCapabilities.getLimitsByMatrix(identifier), bounds, zoom);
            int maxCol = tileMatrix.getMaxCol();
            int maxRow = tileMatrix.getMaxRow();
            int minCol = tileMatrix.getMinCol();
            int minRow = tileMatrix.getMinRow();
            for (int x = minCol; x <= maxCol; x++) {
                for (int y = minRow; y <= maxRow; y++) {
                    list.add(new TileCoordinate(x, y, identifier));
                }
            }
        }

        return list;
    }

    private TileMatrix calculateTileMatrixByExtent(TileMatrix tileMatrixOrig, double[] extent, int zoom) {
        int tileSize = 256;
        double originX = Constants.originX3857;
        double originY = Constants.originY3857;
        double resolution = Constants.globalSize3857 / (tileSize * Math.pow(2, zoom));

        int tileMinX = Integer.MAX_VALUE;
        int tileMaxX = Integer.MIN_VALUE;
        int tileMinY = Integer.MAX_VALUE;
        int tileMaxY = Integer.MIN_VALUE;

        for (int x = tileMatrixOrig.getMinCol(); x <= tileMatrixOrig.getMaxCol(); x++) {
                double minX = originX + x * tileSize * resolution;
                double maxX = originX + (x + 1) * tileSize * resolution;

                if (maxX < extent[0] || minX > extent[2]) {
                	continue;
                }
                if (x < tileMinX) {
                	tileMinX = x;
                }
                if (x > tileMaxX) {
                	tileMaxX = x;
                }

                for (int y = tileMatrixOrig.getMinRow(); y <= tileMatrixOrig.getMaxRow(); y++) {
                    double maxY = originY - y * tileSize * resolution;
                    double minY = originY - (y + 1) * tileSize * resolution;
					
                    if (maxY < extent[1] || minY > extent[3]) {
                    	continue;
                    }
                    if (y < tileMinY) {
                		tileMinY = y;
		            }
		            if (y > tileMaxY) {
		            	tileMaxY = y;
		            }
                }
           }

        return new TileMatrix(tileMatrixOrig.getMatrix(), tileMinY, tileMaxY, tileMinX, tileMaxX);
    }

    private double[] calculateBounds(TileServiceDto service, List<LayerCapabilities> layers) {
        double[] bounds = {service.getMinLon(), service.getMinLat(), service.getMaxLon(), service.getMaxLat()};
        for (LayerCapabilities lc : layers) {
            if (lc.getMinLon() != null && lc.getMinLon() < bounds[0]) {
                bounds[0] = lc.getMinLon();
            }
            if (lc.getMinLat() != null && lc.getMinLat() < bounds[1]) {
                bounds[1] = lc.getMinLat();
            }
            if (lc.getMaxLon() != null && lc.getMaxLon() > bounds[2]) {
                bounds[2] = lc.getMaxLon();
            }
            if (lc.getMaxLat() != null && lc.getMaxLat() > bounds[3]) {
                bounds[3] = lc.getMaxLat();
            }
        }
        return bounds;
    }

    private int invertTileRow(int tileRow, int zoom) {
        return ((1 << zoom) - 1 - tileRow);
    }

    private int parseZoom (String zoom) {
        int result = 0;
        if (zoom.contains(":")) {
            String[] parts = zoom.split(":");
            result = Integer.parseInt(parts[parts.length - 1]);
        }
        return result;
    }

    public BufferedImage getTileImage(TileServiceDto service, String layer, TileCoordinate coordinate) {
        byte[] tileData = downloadTile(service.getUrl(), layer, service.getMatrixSet(),
            coordinate.getX(), coordinate.getY(), coordinate.getZ());

        if (tileData != null) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(tileData));
                return image;
            } catch (IOException e) {
                log.error("Error decoding image");
                e.printStackTrace();
            }
        }
        return null;
    }

    private byte[] downloadTile(String urlService, String layer, String matrixSet, int x, int y, String zoom) {
        String fullUrl = "";
        try {
            fullUrl = getFullUrl(urlService, layer, matrixSet, x, y, zoom);
            if (fullUrl != null) {
                URL url = new URL(fullUrl);
                BufferedImage image = ImageIO.read(url);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            log.error("Failed to download tile from URL: " + fullUrl);
            //e.printStackTrace();
        }
        return null;
    }

    private String getFullUrl(String urlService, String layer, String matrixSet, int x, int y, String zoom) {
        String url = String.format(TEMPLATE, urlService, layer, matrixSet, zoom, y, x);
        return url;
    }

    private double getAvgTileSize(TileServiceDto service, String layer, List<TileCoordinate> coordinates) {
        double estimation = 0;
        Map<String, List<TileCoordinate>> splitedCoordinates = new HashMap<>();

        coordinates.forEach(c -> {
            if (splitedCoordinates.containsKey(c.getZ())) {
                splitedCoordinates.get(c.getZ()).add(c);
            } else {
                List<TileCoordinate> zoomCoords = new ArrayList<>();
                zoomCoords.add(c);
                splitedCoordinates.put(c.getZ(), zoomCoords);
            }
        });

        for( Map.Entry<String, List<TileCoordinate>> entry : splitedCoordinates.entrySet()) {
            List<TileCoordinate> value = entry.getValue();
            int maxX = value.stream().map(c -> c.getX()).max(Comparator.naturalOrder()).get();
            int minX = value.stream().map(c -> c.getX()).min(Comparator.naturalOrder()).get();
            int maxY = value.stream().map(c -> c.getY()).max(Comparator.naturalOrder()).get();
            int minY = value.stream().map(c -> c.getY()).min(Comparator.naturalOrder()).get();
            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;
            int coordsSize = value.size();
            List<int[]> sampleCoords = new ArrayList<>();
            sampleCoords.add(new int[]{minX, minY});
            sampleCoords.add(new int[]{maxX, maxY});
            sampleCoords.add(new int[]{centerX, centerY});
            sampleCoords.add(new int[]{centerX - 1, centerY + 1});
            sampleCoords.add(new int[]{centerX + 1, centerY - 1});
            int sampleSize = 0;
            String zoom = entry.getKey();
            int bytes = 0;
            byte[] tile = null;
            for (int[] coord : sampleCoords) {
                int col = coord[0];
                int row = coord[1];
                tile = downloadTile(service.getUrl(), layer, service.getMatrixSet(), col, row, zoom);
                if (tile != null) {
                    bytes += tile.length;
                    sampleSize++;
                }
            }
            double zoomEstimation = sampleSize > 0 ? (bytes / sampleSize) : 0;
            log.info("AvgSize " + zoom + " -> " + (zoomEstimation / 1024) + "KB (" + coordsSize + " tiles)");
            estimation += zoomEstimation * coordsSize;
        };

        return estimation;
    }
}
