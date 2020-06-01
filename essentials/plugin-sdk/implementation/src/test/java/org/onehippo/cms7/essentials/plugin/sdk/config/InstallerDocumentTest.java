/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.config;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @version "$Id$"
 */
public class InstallerDocumentTest extends BaseTest {

    @Inject private PluginFileService pluginFileService;

    @Test
    public void testGetPluginId() throws Exception {
        final String filename = "foo.bar.zar.MyBean";
        final Calendar today = Calendar.getInstance();
        final InstallerDocument document = new InstallerDocument();
        document.setDateInstalled(today);
        pluginFileService.write(filename, document);

        final InstallerDocument fetched = pluginFileService.read(filename, InstallerDocument.class);
        assertNotNull(fetched.getDateInstalled());
        assertEquals(fetched.getDateInstalled().getTime(), today.getTime());
    }
}
