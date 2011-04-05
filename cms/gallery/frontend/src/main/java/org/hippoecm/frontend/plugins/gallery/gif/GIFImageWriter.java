/*
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * Licensed under the Helma License (the "License"); you may not use this
 * file except in compliance with the License. A copy of the License is
 * available at http://dev.helma.org/license/
 *
 * The imageio integration is inspired by the package org.freehep.graphicsio.gif
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * See NOTICE.txt for licensing information
 */
package org.hippoecm.frontend.plugins.gallery.gif;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.DataOutput;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

public class GIFImageWriter extends ImageWriter {
    GIFEncoder encoder;

    public GIFImageWriter(GIFImageWriterSpi originatingProvider) {
        super(originatingProvider);
        encoder = new GIFEncoder();
    }

    public void write(IIOMetadata streamMetadata, IIOImage image,
        ImageWriteParam param) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("image == null");
        }
        if (image.hasRaster()) {
            throw new UnsupportedOperationException("Cannot write rasters");
        }
        Object output = getOutput();
        if (output == null) {
            throw new IllegalStateException("output was not set");
        }
        if (param == null) {
            param = getDefaultWriteParam();
        }
        RenderedImage ri = image.getRenderedImage();
        if (!(ri instanceof BufferedImage)) {
            throw new IOException("RenderedImage is not a BufferedImage");
        }
        if (!(output instanceof DataOutput)) {
            throw new IOException("output is not a DataOutput");
        }
        encoder.encode((BufferedImage) ri, (DataOutput) output,
            param.getProgressiveMode() != ImageWriteParam.MODE_DISABLED, null);
    }

    public IIOMetadata convertStreamMetadata(IIOMetadata inData,
        ImageWriteParam param) {
        return null;
    }

    public IIOMetadata convertImageMetadata(IIOMetadata inData,
        ImageTypeSpecifier imageType, ImageWriteParam param) {
        return null;
    }

    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
        ImageWriteParam param) {
        return null;
    }

    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        return null;
    }

    public ImageWriteParam getDefaultWriteParam() {
        return new GIFImageWriteParam(getLocale());
    }
}
