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

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.service.MavenDependencyService;
import org.onehippo.cms7.essentials.dashboard.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.dashboard.services.MavenDependencyServiceImpl;
import org.onehippo.cms7.essentials.dashboard.services.MavenRepositoryServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class EnterpriseUtilsTest extends ResourceModifyingTest {

    final MavenDependencyService dependencyService = new MavenDependencyServiceImpl();
    final MavenRepositoryService repositoryService = new MavenRepositoryServiceImpl();

    @Test
    public void testUpgradeToEnterpriseProject() throws Exception {
        final PluginContext context = getContext();
        File projectPomXml = createModifiableFile("/project/pom.xml", "pom.xml");
        createModifiableFile("/project/cms/pom.xml", "cms/pom.xml");

        String before = contentOf(projectPomXml);
        assertFalse(before.contains("<id>" + ProjectUtils.ENT_REPO_ID + "</id>"));
        assertFalse(before.contains("<name>" + ProjectUtils.ENT_REPO_NAME + "</name>"));
        assertFalse(before.contains("<url>" + ProjectUtils.ENT_REPO_URL + "</url>"));

        assertTrue(EnterpriseUtils.upgradeToEnterpriseProject(context, dependencyService, repositoryService));
        assertTrue(EnterpriseUtils.upgradeToEnterpriseProject(context, dependencyService, repositoryService));

        String after = contentOf(projectPomXml);
        assertEquals(1, StringUtils.countMatches(after, "<id>" + ProjectUtils.ENT_REPO_ID + "</id>"));
        assertEquals(1, StringUtils.countMatches(after, "<name>" + ProjectUtils.ENT_REPO_NAME + "</name>"));
        assertEquals(1, StringUtils.countMatches(after, "<url>" + ProjectUtils.ENT_REPO_URL + "</url>"));
    }
}
