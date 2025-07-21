package org.sitmun.proxy.middleware.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Capabilities {
    String tileMatrixSet;
    List<LayerCapabilities> layers;

    public void addLayer(LayerCapabilities layerCapabilities) {
        if (layers == null) {
            layers = new ArrayList<>();
        }
        layers.add(layerCapabilities);
    }
}