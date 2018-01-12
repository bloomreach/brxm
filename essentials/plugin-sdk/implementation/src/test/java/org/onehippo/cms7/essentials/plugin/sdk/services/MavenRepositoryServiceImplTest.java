/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.essentials.plugin.sdk.model.MavenRepository;
import org.onehippo.cms7.essentials.plugin.sdk.service.model.Module;
import org.onehippo.cms7.essentials.plugin.sdk.service.MavenRepositoryService;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenRepositoryServiceImplTest extends ResourceModifyingTest {

    @Inject private MavenRepositoryService service;

    @Test
    public void test_add_repository() throws Exception {
        File pomXml = createModifiableFile("/services/mavenrepository/pom.xml", "pom.xml");

        final MavenRepository repository = new MavenRepository();
        repository.setId("test");
        repository.setName("Test Repository");
        repository.setUrl("https://my.test.url/foo/");
        final MavenRepository.Policy releasePolicy = new MavenRepository.Policy();
        releasePolicy.setEnabled("true");
        releasePolicy.setUpdatePolicy("update-policy");
        releasePolicy.setChecksumPolicy("checksum-policy");
        repository.setReleasePolicy(releasePolicy);
        final MavenRepository.Policy snapshotPolicy = new MavenRepository.Policy();
        snapshotPolicy.setEnabled("true");
        repository.setSnapshotPolicy(snapshotPolicy);

        String before = contentOf(pomXml);
        assertFalse(before.contains("<id>test</id>"));
        assertFalse(before.contains("<name>Test Repository</name>"));
        assertFalse(before.contains("<url>https://my.test.url/foo/</url>"));
        assertFalse(before.contains("<updatePolicy>update-policy</updatePolicy>"));
        assertFalse(before.contains("<checksumPolicy>checksum-policy</checksumPolicy>"));
        assertFalse(before.contains("<snapshots />"));

        assertTrue(service.addRepository(Module.PROJECT, repository));
        assertTrue(service.addRepository(Module.PROJECT, repository));

        String after = contentOf(pomXml);
        assertEquals(1, StringUtils.countMatches(after, "<id>test</id>"));
        assertEquals(1, StringUtils.countMatches(after, "<name>Test Repository</name>"));
        assertEquals(1, StringUtils.countMatches(after, "<url>https://my.test.url/foo/</url>"));
        assertEquals(1, StringUtils.countMatches(after, "<updatePolicy>update-policy</updatePolicy>"));
        assertEquals(1, StringUtils.countMatches(after, "<checksumPolicy>checksum-policy</checksumPolicy>"));
        assertEquals(1, StringUtils.countMatches(after, "<snapshots />"));
    }

    @Test
    public void test_invalid_input() throws Exception {
        createModifiableFile("/services/mavenrepository/pom.xml", "pom.xml");

        final MavenRepository repository = new MavenRepository();
        repository.setId("test");
        repository.setName("Test Repository");
        // no URL

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(MavenRepositoryServiceImpl.class).build()) {
            assertFalse(service.addRepository(Module.PROJECT, repository));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Failed to add Maven repository 'MavenRepository{id='test', name='Test Repository', url='null'}'"
                    + " to module 'project', no repository URL specified.")));
        }
    }

    @Test
    public void test_invalid_pom() throws Exception {
        createModifiableFile("/services/mavenrepository/pom.xml", "foo.xml");

        final MavenRepository repository = new MavenRepository();
        repository.setUrl("boo");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(MavenRepositoryServiceImpl.class).build()) {
            assertFalse(service.addRepository(Module.PROJECT, repository));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Unable to load model for pom.xml of module 'project'.")));
        }
    }
}
