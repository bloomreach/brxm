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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is used by {@link org.hippoecm.frontend.plugins.gallery.imageutil.ImageMetaData} to able to parse the
 * InputStream twice (which is required when trying to detect YCCK or CMYK as Sanselan marks both as CMYK.
 *
 * @deprecated Since the introduction of the {@link ImageBinary} this class has been deprecated as it exposes
 * {@link javax.jcr.Binary#getStream()} which is far more convenient to use.
 */
@Deprecated
public class ReusableInputStream extends BufferedInputStream {

    private boolean canBeClosed;

    public ReusableInputStream(InputStream in) {
        super(in);
        mark(Integer.MAX_VALUE);
    }

    @Override
    public void close() throws IOException {
        if(canBeClosed) {
            super.close();
        }
    }

    public void canBeClosed() {
        canBeClosed = true;
    }
}
