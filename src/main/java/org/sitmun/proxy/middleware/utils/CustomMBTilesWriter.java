package org.sitmun.proxy.middleware.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import ch.poole.geo.mbtiles4j.MBTilesWriteException;
import ch.poole.geo.mbtiles4j.MBTilesWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomMBTilesWriter extends MBTilesWriter {

    public CustomMBTilesWriter(File f) throws MBTilesWriteException {
        super(f);
    }

    public void updateTile(int zoom, int column, int row, byte[] bytes) throws MBTilesWriteException {
        try {
            PreparedStatement stmt = getConnection().prepareStatement("UPDATE tiles SET tile_data = ? WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?");
            stmt.setBytes(1, bytes);
            stmt.setInt(2, zoom);
            stmt.setInt(3, column);
            stmt.setInt(4, row);
            int updates = stmt.executeUpdate();
            stmt.close();
            log.info("Updated " + updates + " tile(s) at zoom: " + zoom + ", column: " + column + ", row: " + row);
        } catch (SQLException e) {
            throw new MBTilesWriteException("Update Tile failed.", e);
        }
    }

    public BufferedImage combineTiles(BufferedImage tile1, BufferedImage tile2) {
        if (tile1 == null) {
            return tile2;
        }
        if (tile2 == null) {
            return tile1;
        }
        log.info("Combinando tiles");

        int width = Math.max(tile1.getWidth(), tile2.getWidth());
        int height = Math.max(tile1.getHeight(), tile2.getHeight());
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = combined.createGraphics();
        g2d.drawImage(tile1, 0, 0, null);
        g2d.drawImage(tile2, 0, 0, null);
        return combined;
    }

}
