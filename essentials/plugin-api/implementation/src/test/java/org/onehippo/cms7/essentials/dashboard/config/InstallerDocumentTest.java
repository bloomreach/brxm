/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.Calendar;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.TestPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @version "$Id$"
 */
public class InstallerDocumentTest extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(InstallerDocumentTest.class);

    @Test
    public void testGetPluginId() throws Exception {

        final InstallerDocument document = new InstallerDocument();
        final String pluginId = "foo.bar.zar.MyBean";
        document.setName(pluginId);
        log.info("document {}", document);
        document.setPluginId("test.foo");
        final Calendar today = Calendar.getInstance();
        document.setDateInstalled(today);
        final TestPluginContext context = (TestPluginContext) getContext();
        final InstallerDocument fetched;
        try (PluginConfigService manager = new FilePluginService(context)) {
            manager.write(document);
            fetched = manager.read(pluginId, InstallerDocument.class);
        }
        assertNotNull(fetched.getDateInstalled());
        assertEquals(fetched.getDateInstalled().getTime(), today.getTime());
        assertNotNull(fetched.getPluginId());
    }
}
