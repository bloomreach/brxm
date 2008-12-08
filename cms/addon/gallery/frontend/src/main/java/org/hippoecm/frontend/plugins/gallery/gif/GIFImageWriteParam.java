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

import java.util.Locale;
import java.util.Properties;

import javax.imageio.ImageWriteParam;

public class GIFImageWriteParam extends ImageWriteParam {
    private boolean quantizeColors;
    private String quantizeMode;

    public GIFImageWriteParam(Locale locale) {
        super(locale);
        canWriteProgressive = true;
        progressiveMode = MODE_DEFAULT;
    }

    public ImageWriteParam getWriteParam(Properties properties) {
        return this;
    }
}

