/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.onehippo.repository.bootstrap.util;

import java.io.InputStream;
import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BundleFileInfoTest {

    @Test
    public void readBundleFileInfo() throws Exception {
        try (final InputStream in = getClass().getResourceAsStream("/bootstrap/resourcebundle.json")) {
            final BundleFileInfo bundleFileInfo = BundleFileInfo.readInfo(in);
            assertEquals(2, bundleFileInfo.getBundleInfos().size());
            final BundleInfo bundleInfo = bundleFileInfo.getBundleInfos().iterator().next();
            assertEquals(Locale.ENGLISH, bundleInfo.getLocale());
            assertEquals("foo.bar", bundleInfo.getName());
            assertEquals(1, bundleInfo.getTranslations().size());
            assertEquals("value", bundleInfo.getTranslations().get("key"));
        }
    }

}
