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

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

public class GIFImageWriterSpi extends ImageWriterSpi {

    public GIFImageWriterSpi() {
        super(
            "Hippo ECM image gallery plugin (http://docs.onehippo.org)",
            "1.0",
            new String[] {"gif", "GIF"},
            new String[] {"gif", "GIF"},
            new String[] {"image/gif", "image/x-gif"},
            "org.hippoecm.frontend.plugins.gallery.gif.GIFImageWriter",
            STANDARD_OUTPUT_TYPE,
            null,
            false, null, null, null, null,
            false, null, null, null, null
        );
    }

    public String getDescription(Locale locale) {
        return "Graphics Interchange Format";
    }

    public ImageWriter createWriterInstance(Object extension)
        throws IOException {
        return new GIFImageWriter(this);
    }

    public boolean canEncodeImage(ImageTypeSpecifier type) {
        // FIXME handle # colors
        return true;
    }
}

