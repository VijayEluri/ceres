package com.bc.ceres.glayer.support;

import static org.junit.Assert.*;
import org.junit.Test;
import static com.bc.ceres.glayer.Assert2D.*;
import com.bc.ceres.glayer.LayerTest;
import com.bc.ceres.glevel.LevelImage;
import com.bc.ceres.glevel.support.NullLevelImage;
import com.bc.ceres.grender.Rendering;

import javax.media.jai.TiledImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class ImageLayerTest  {

    @Test
    public void testThatLayerOperattesWithNullImage() {
        final ImageLayer layer = new ImageLayer(LevelImage.NULL);

        assertNotNull(layer.getLevelImage());
        assertNotNull(layer.getImage());

        assertEquals(1, layer.getLevelCount());

        assertNotNull(layer.getImageToModelTransform());
        assertTrue(layer.getImageToModelTransform().isIdentity());

        assertNotNull(layer.getModelToImageTransform());
        assertTrue(layer.getModelToImageTransform().isIdentity());

        assertNotNull(layer.getBounds());
        assertTrue(layer.getBounds().isEmpty());
    }

    @Test
    public void testConstructors() {
        ImageLayer layer;
        final TiledImage image = new TiledImage(new BufferedImage(320, 200, BufferedImage.TYPE_BYTE_GRAY), true);

        layer = new ImageLayer(image);
        assertSame(image, layer.getImage());
        assertEquals(new AffineTransform(), layer.getModelToImageTransform());
        assertEquals(new AffineTransform(), layer.getImageToModelTransform());
        assertEquals(1, layer.getLevelCount());


        final AffineTransform i2m = AffineTransform.getTranslateInstance(+100, +200);
        layer = new ImageLayer(image, i2m);
        assertSame(image, layer.getImage());
        assertNotSame(i2m, layer.getImageToModelTransform());
        assertNotSame(i2m, layer.getModelToImageTransform());
        assertEquals(AffineTransform.getTranslateInstance(+100, +200), layer.getImageToModelTransform());
        assertEquals(AffineTransform.getTranslateInstance(-100, -200), layer.getModelToImageTransform());
        assertEquals(1, layer.getLevelCount());
    }

    @Test
    public void testBoundingBox() {
        ImageLayer layer;
        final TiledImage image = new TiledImage(new BufferedImage(320, 200, BufferedImage.TYPE_BYTE_GRAY), true);

        layer = new ImageLayer(image);
        assertNotNull(layer.getBounds());
        assertEquals(new Rectangle2D.Double(0.0, 0.0, 320.0, 200.0), layer.getBounds());

        final AffineTransform i2m = new AffineTransform(0.5, 0, 0, 0.5, -25.5, 50.3);
        layer = new ImageLayer(image, i2m);
        assertNotNull(layer.getBounds());
        assertEquals(new Rectangle2D.Double(-25.5, 50.3, 160.0, 100.0), layer.getBounds());
    }
}