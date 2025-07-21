package org.sitmun.proxy.middleware.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LayerCapabilities {
    String layerIdentifier;
    List<TileMatrix> limits;
    double minLon;
    double minLat;
    double maxLon;
    double maxLat;

    public TileMatrix getLimitsByMatrix(String matrix) {
        return limits.stream().filter(l -> l.getMatrix().equals(matrix)).findFirst().orElse(null);
    }

    public void addTileMatrix(String identifier, int minTileRow, int maxTileRow, int minTileCol, int maxTileCol) {
        TileMatrix tileMatrix = new TileMatrix(identifier, minTileRow, maxTileRow, minTileCol, maxTileCol);
        if (limits == null) {
            limits = new ArrayList<>();
        }
        limits.add(tileMatrix);
    }
}
