/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.MavenModelService;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenModelServiceImplTest extends ResourceModifyingTest {

    @Inject private MavenModelService modelService;

    @Test
    public void update_parent() throws Exception {
        String groupId = "testGroupId";
        String artifactId = "testArtifactId";
        String version = "testVersion";
        File pomXml = createModifiableFile("/services/mavenmodel/pom.xml", "pom.xml");

        String before = contentOf(pomXml);
        assertFalse(before.contains(groupId));
        assertFalse(before.contains(artifactId));
        assertFalse(before.contains(version));

        assertTrue(modelService.setParentProject(Module.PROJECT, groupId, artifactId, version));

        String after = contentOf(pomXml);
        assertEquals(1, StringUtils.countMatches(after, groupId));
        assertEquals(1, StringUtils.countMatches(after, artifactId));
        assertEquals(1, StringUtils.countMatches(after, version));

        assertTrue(modelService.setParentProject(Module.PROJECT, groupId, artifactId, null));

        String after2 = contentOf(pomXml);
        assertEquals(1, StringUtils.countMatches(after2, groupId));
        assertEquals(1, StringUtils.countMatches(after2, artifactId));
        assertEquals(1, StringUtils.countMatches(after2, version));
    }

    @Test
    public void no_parent() throws Exception {
        createModifiableFile("/services/mavenmodel/pom-no-parent.xml", "pom.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(MavenModelServiceImpl.class).build()) {
            assertFalse(modelService.setParentProject(Module.PROJECT, "groupId", "artifactId", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to adjust parent model: no parent element found in POM.")));
        }
    }

    @Test
    public void no_pom() throws Exception {
        createModifiableFile("/services/mavenmodel/pom.xml", "pim.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(MavenModelUtils.class).build()) {
            assertFalse(modelService.setParentProject(Module.PROJECT, "groupId", "artifactId", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Error parsing pom")));
        }
    }
}
