package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.IOUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ScaleImageOperation}.
 */
public class ScaleImageOperationTest {

    @Test
    public void scaleLandscapeImageInLandscapeBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-688x292.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 200, 84);
    }

    @Test
    public void scaleLandscapeImageInPortraitBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-688x292.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(100, 200, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 100, 42);
    }

    @Test
    public void scalePortraitImageInLandscapeBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 88, 100);
    }

    @Test
    public void scalePortraitImageInPortraitBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(100, 200, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 100, 112);
    }

    @Test
    public void scaleUp() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(500, 500, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 443, 500);
    }

    @Test
    public void useOriginalDataWhenNoBoundingBoxIsGiven() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 380, 428);

        // redo scaling to check the data itself
        data = getClass().getResourceAsStream("/test-380x428.jpg");
        scaleOp = new ScaleImageOperation(0, 0, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        InputStream original = getClass().getResourceAsStream("/test-380x428.jpg");
        assertTrue("Original image data should be used as-is", IOUtils.contentEquals(original, scaleOp.getScaledData()));
    }

    @Test
    public void useOriginalDataWhenBoundingBoxMatchesOriginalDimensions() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(380, 428, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 380, 428);

        // redo scaling to check the data itself
        data = getClass().getResourceAsStream("/test-380x428.jpg");
        scaleOp = new ScaleImageOperation(380, 428, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        InputStream original = getClass().getResourceAsStream("/test-380x428.jpg");
        assertTrue("Original image data should be used as-is", IOUtils.contentEquals(original, scaleOp.getScaledData()));
    }

    @Test
    public void useOriginalDataWhenScalingUpAndUpscalingIsDisabled() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(500, 500, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 380, 428);

        // redo scaling to check the data itself
        data = getClass().getResourceAsStream("/test-380x428.jpg");
        scaleOp = new ScaleImageOperation(500, 500, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        InputStream original = getClass().getResourceAsStream("/test-380x428.jpg");
        assertTrue("Original image data should be used as-is", IOUtils.contentEquals(original, scaleOp.getScaledData()));
    }

    @Test
    public void ensureMinimumWidthOfOne() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-1x5000.png");
        ScaleImageOperation scaleOp = new ScaleImageOperation(60, 60, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/png");
        checkImageDimensions(scaleOp, "image/png", 1, 60);
    }

    @Test
    public void ensureMinimumHeightOfOne() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-5000x1.png");
        ScaleImageOperation scaleOp = new ScaleImageOperation(60, 60, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/png");
        checkImageDimensions(scaleOp, "image/png", 60, 1);
    }

    @Test
    public void scaleGif() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.gif");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/gif");
        checkImageDimensions(scaleOp, "image/gif", 88, 100);
    }

    private void checkImageDimensions(ScaleImageOperation scaleOp, String mimeType, int expectedWidth, int expectedHeight) throws IOException {
        assertEquals(expectedWidth, scaleOp.getScaledWidth());
        assertEquals(expectedHeight, scaleOp.getScaledHeight());

        ImageReader reader = ImageIO.getImageReadersByMIMEType(mimeType).next();
        ImageInputStream iis = null;
        try {
            iis = ImageIO.createImageInputStream(scaleOp.getScaledData());
            reader.setInput(iis);
            assertEquals(scaleOp.getScaledWidth(), reader.getWidth(0));
            assertEquals(scaleOp.getScaledHeight(), reader.getHeight(0));
        } finally {
            if (iis != null) {
                iis.close();
            }
        }
    }

}
