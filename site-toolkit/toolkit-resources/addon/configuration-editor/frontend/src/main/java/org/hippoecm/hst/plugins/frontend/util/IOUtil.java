package org.hippoecm.hst.plugins.frontend.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.Resource;
import org.apache.wicket.markup.html.image.resource.DynamicImageResource;
import org.apache.wicket.util.io.ByteArrayOutputStream;

public class IOUtil {

    public static byte[] obtainByteData(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] bytes = new byte[512];

        // Read bytes from the input stream in bytes.length-sized chunks and write
        // them into the output stream
        int readBytes;
        while ((readBytes = is.read(bytes)) > 0) {
            os.write(bytes, 0, readBytes);
        }

        // Convert the contents of the output stream into a byte array
        byte[] byteData = os.toByteArray();

        // Close the streams
        is.close();
        os.close();

        return byteData;
    }

    public static Resource obtainResource(InputStream is) throws IOException {
        final byte[] imageData = obtainByteData(is);
        return new DynamicImageResource() {
            private static final long serialVersionUID = 1L;

            @Override
            protected byte[] getImageData() {
                return imageData;
            }

        };
    }

    /*
    public static Resource obtainScaledImageResource(InputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        image = createCompatibleImage(image);
        image = resize(image, 100, 100);
        image = blurImage(image);
        image = resize(image, 100, 100);
        ImageIO.write(image, formatName, output)
        
        return image;
    }

    public static BufferedImage blurImage(BufferedImage image) {
        float ninth = 1.0f / 9.0f;
        float[] blurKernel = { ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth };

        Map map = new HashMap();

        map.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        map.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RenderingHints hints = new RenderingHints(map);
        BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, blurKernel), ConvolveOp.EDGE_NO_OP, hints);
        return op.filter(image, null);
    }

    private static BufferedImage createCompatibleImage(BufferedImage image) {
        GraphicsConfiguration gc = BufferedImageGraphicsConfig.getConfig(image);
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage result = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        Graphics2D g2 = result.createGraphics();
        g2.drawRenderedImage(image, null);
        g2.dispose();
        return result;
    }

    private static BufferedImage resize(BufferedImage image, int width, int height) {
        int type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.setComposite(AlphaComposite.Src);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }
    */
}
