/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.webfiles.vault;

import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.junit.Test;
import org.onehippo.cms7.services.webfiles.vault.DefaultBundleRootContentXmlEntry;

import static org.junit.Assert.*;

public class DefaultBundleRootContentXmlEntryTest {

    @Test
    public void openInputStream() {
        final InputStream inputStream = DefaultBundleRootContentXmlEntry.getInstance().openInputStream();
        assertNotNull(inputStream);
    }

    @Test
    public void getInputSource() {
        final VaultInputSource inputSource = DefaultBundleRootContentXmlEntry.getInstance().getInputSource();
        assertTrue("content length is greater than zero", inputSource.getContentLength() > 0);
        assertTrue("last modified time is greater than zero", inputSource.getLastModified() > 0);
    }

}
