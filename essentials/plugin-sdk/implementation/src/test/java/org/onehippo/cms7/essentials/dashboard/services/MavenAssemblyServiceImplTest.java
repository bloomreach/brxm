/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.dashboard.model.MavenDependency;
import org.onehippo.cms7.essentials.dashboard.utils.Dom4JUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenAssemblyServiceImplTest extends ResourceModifyingTest {

    private MavenAssemblyServiceImpl service = new MavenAssemblyServiceImpl();

    @Test
    public void add_dependency_set() throws Exception {
        File descriptor = createModifiableFile("/services/mavenassembly/descriptor.xml",
                "src/main/assembly/descriptor.xml");

        String before = contentOf(descriptor);
        assertFalse(before.contains("<dependencySets>"));

        MavenDependency dependency = new MavenDependency("group", "artifact");
        assertTrue(service.addDependencySet("descriptor.xml", "outDir",
                "file.war", true, "scope", dependency));

        String after = contentOf(descriptor);
        assertEquals(1, StringUtils.countMatches(after, "<include>group:artifact</include>"));
        assertEquals(1, StringUtils.countMatches(after, "<useProjectArtifact>true</useProjectArtifact>"));

        // add second dependency
        assertTrue(service.addDependencySet("descriptor.xml", "outDir",
                "file.war", false, "scope", dependency));

        after = contentOf(descriptor);
        assertEquals(2, StringUtils.countMatches(after, "<include>group:artifact</include>"));
        assertEquals(1, StringUtils.countMatches(after, "<useProjectArtifact>true</useProjectArtifact>"));
        assertEquals(1, StringUtils.countMatches(after, "<useProjectArtifact>false</useProjectArtifact>"));
    }

    @Test
    public void add_dependency_no_descriptor() throws Exception {
        createModifiableFile("/services/mavenassembly/descriptor.xml",
                "src/main/assembly/descriptor.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addDependencySet("dummy.xml", null,
                    null, false, null, null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }

    @Test
    public void add_include_to_first_dependency_set() throws Exception {
        File descriptor = createModifiableFile("/services/mavenassembly/descriptor-with-dependency-sets.xml",
                "src/main/assembly/descriptor.xml");

        String before = contentOf(descriptor);
        assertFalse(before.contains("<include>group:artifact</include>"));

        MavenDependency dependency = new MavenDependency("group", "artifact");
        assertTrue(service.addIncludeToFirstDependencySet("descriptor.xml", dependency));

        String after = contentOf(descriptor);
        assertEquals(1, StringUtils.countMatches(after, "<include>group:artifact</include>"));
        assertEquals(1, StringUtils.countMatches(after, "<dependencySet>second</dependencySet"));

        // prevent duplicates
        assertTrue(service.addIncludeToFirstDependencySet("descriptor.xml", dependency));
        assertEquals(1, nrOfOccurrences(descriptor, "<include>group:artifact</include>"));
    }

    @Test
    public void add_include_bad_xml() throws Exception {
        createModifiableFile("/services/mavenassembly/descriptor-invalid.xml",
                "src/main/assembly/descriptor.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(Dom4JUtils.class).build()) {
            assertFalse(service.addIncludeToFirstDependencySet("descriptor.xml", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to update XML file")));
        }
    }
}
