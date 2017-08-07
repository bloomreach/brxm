/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.List;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class ProjectUtilsTest extends BaseResourceTest {

    @Test
    public void testSitePackages() throws Exception {
        final PluginContext context = getContext();

        final List<String> sitePackages = ProjectUtils.getSitePackages(context);
        assertTrue(sitePackages.size() > 8);
        assertTrue(sitePackages.contains("org"));
        assertTrue(sitePackages.contains("org.dummy"));

        // validate defaults
        assertEquals("repository-data", ProjectUtils.getRepositoryDataFolder(context).getName());
        assertEquals("config", ProjectUtils.getRepositoryDataConfigFolder(context).getName());
        assertEquals("content", ProjectUtils.getRepositoryDataContentFolder(context).getName());
        assertEquals("webfiles", ProjectUtils.getRepositoryDataWebfilesFolder(context).getName());
    }
}
