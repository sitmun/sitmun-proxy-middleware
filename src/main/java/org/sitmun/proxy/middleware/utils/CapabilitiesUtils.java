package org.sitmun.proxy.middleware.utils;

import java.net.URL;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.sitmun.proxy.middleware.dto.Capabilities;
import org.sitmun.proxy.middleware.dto.LayerCapabilities;
import org.sitmun.proxy.middleware.dto.TileServiceDto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CapabilitiesUtils {
    
    public static Capabilities parseWMTSCapabilities(TileServiceDto service) throws Exception{
        String capabilitiesUrl = service.getUrl() + "?SERVICE=" + service.getType() + "&REQUEST=GetCapabilities";
        log.info("Capabilities URL: " + capabilitiesUrl);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL(capabilitiesUrl).openStream());
        doc.getDocumentElement().normalize();
        List<String> layers = service.getLayers();
        NodeList capLayers = doc.getElementsByTagName("Layer");
        Element layer = null;
        Capabilities capabilities = new Capabilities();
        capabilities.setTileMatrixSet(service.getMatrixSet());
        for (int i = 0; i < capLayers.getLength(); i++) {
            layer = (Element) capLayers.item(i);
            String layerIdentifier = layer.getElementsByTagName("Identifier").item(0).getTextContent();
            if (layers.contains(layerIdentifier)) {
                capabilities.addLayer(createWMTSLayerCapabilities(layer, capabilities, capabilities.getTileMatrixSet()));
            }
        }

        return capabilities;
    }

    private static LayerCapabilities createWMTSLayerCapabilities(Element layer, Capabilities capabilities, String tileMatrixSet) {
        LayerCapabilities layerCapabilities = new LayerCapabilities();
        String[] lowerCorner = layer.getElementsByTagName("LowerCorner").item(0).getTextContent().split(" ");
        String[] upperCorner = layer.getElementsByTagName("UpperCorner").item(0).getTextContent().split(" ");
        layerCapabilities.setMinLon(Double.parseDouble(lowerCorner[0]));
        layerCapabilities.setMinLat(Double.parseDouble(lowerCorner[1]));
        layerCapabilities.setMaxLon(Double.parseDouble(upperCorner[0]));
        layerCapabilities.setMaxLat(Double.parseDouble(upperCorner[1]));
        NodeList tileMatrixSetLinks = layer.getElementsByTagName("TileMatrixSetLink");
        for (int i = 0; i < tileMatrixSetLinks.getLength(); i++) {
            Element matrixSet = (Element)tileMatrixSetLinks.item(i);
            String matrixIdentifier = matrixSet.getElementsByTagName("TileMatrixSet").item(0).getTextContent();
            if (matrixIdentifier.equals(tileMatrixSet)) {
                NodeList limits = matrixSet.getElementsByTagName("TileMatrixLimits");
                addLimitsToWMTSLayerCapabilities(layerCapabilities, limits);
            }
        }

        return layerCapabilities;
    }

    private static void addLimitsToWMTSLayerCapabilities(LayerCapabilities layerCapabilities, NodeList limits) {
        for (int i = 0; i < limits.getLength(); i++) {
            Element limit = (Element)limits.item(i);
            String tileMatrix = limit.getElementsByTagName("TileMatrix").item(0).getTextContent();
            String minTileRow = limit.getElementsByTagName("MinTileRow").item(0).getTextContent();
            String maxTileRow = limit.getElementsByTagName("MaxTileRow").item(0).getTextContent();
            String minTileCol = limit.getElementsByTagName("MinTileCol").item(0).getTextContent();
            String maxTileCol = limit.getElementsByTagName("MaxTileCol").item(0).getTextContent();
            layerCapabilities.addTileMatrix(tileMatrix, Integer.parseInt(minTileRow), Integer.parseInt(maxTileRow),
                Integer.parseInt(minTileCol), Integer.parseInt(maxTileCol));
        }
    }
}
