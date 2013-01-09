/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.InputStream;

import org.apache.wicket.util.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ImageUtilTest {

    @Test
    public void convertCMYKToRGB() throws Exception {
        InputStream cmyk = getClass().getResourceAsStream("/test-CMYK.jpg");
        InputStream rgb = ImageUtils.convertToRGB(cmyk, ImageMetaData.ColorModel.CMYK);

        ImageMetaData metaData = new ImageMetaData("image/jpeg", "test.jpg");
        metaData.parse(rgb);
        assertEquals(ImageMetaData.ColorModel.RGB, metaData.getColorModel());

        InputStream cmyk2 = getClass().getResourceAsStream("/test-CMYK.jpg");
        assertFalse(IOUtils.contentEquals(rgb, cmyk2));
    }

    @Test
    public void convertYCCKtoRGB() throws Exception {
        InputStream ycck = getClass().getResourceAsStream("/test-YCCK.jpg");
        InputStream rgb = ImageUtils.convertToRGB(ycck, ImageMetaData.ColorModel.YCCK);

        ImageMetaData metaData = new ImageMetaData("image/jpeg", "test.jpg");
        metaData.parse(rgb);
        assertEquals(ImageMetaData.ColorModel.RGB, metaData.getColorModel());

        InputStream ycck2 = getClass().getResourceAsStream("/test-YCCK.jpg");
        assertFalse(IOUtils.contentEquals(rgb, ycck2));
    }

}
