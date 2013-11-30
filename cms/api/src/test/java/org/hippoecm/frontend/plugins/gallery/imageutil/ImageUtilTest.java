/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.IOException;
import java.io.InputStream;

import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.wicket.util.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ImageUtilTest {

    @Test
    public void convertCMYKToRGB() throws Exception {
        InputStream cmyk = getClass().getResourceAsStream("/test-CMYK.jpg");
        InputStream rgb = ImageUtils.convertToRGB(cmyk, ColorModel.CMYK);

        assertTrue(isRGB(rgb, "test-CMYK.jpg"));
    }

    @Test
    public void convertYCCKtoRGB() throws Exception {
        InputStream ycck = getClass().getResourceAsStream("/test-YCCK.jpg");
        InputStream rgb = ImageUtils.convertToRGB(ycck, ColorModel.YCCK);

        assertTrue(isRGB(rgb, "test-YCCK.jpg"));
    }

    public static boolean isRGB(final InputStream stream, final String fileName) throws IOException, ImageReadException {
        try {
            ImageInfo info = Sanselan.getImageInfo(stream, fileName);
            return ImageInfo.COLOR_TYPE_RGB == info.getColorType();
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }


}
