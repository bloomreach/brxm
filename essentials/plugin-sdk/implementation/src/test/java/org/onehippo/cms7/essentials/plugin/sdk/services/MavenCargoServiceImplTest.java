/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MavenCargoServiceImplTest extends ResourceModifyingTest {

    @Inject private MavenCargoServiceImpl service;

    @Test
    public void addCargoDeployableTest() throws Exception {
        final String webappContext = "/addCargoDeployableTest";

        File pomXml = createModifiableFile("/project/pom.xml", "pom.xml");

        String before = contentOf(pomXml);
        assertFalse(before.contains("<context>" + webappContext + "</context>"));
        assertFalse(before.contains("<groupId>test.group.id</groupId>"));
        assertFalse(before.contains("<artifactId>test-artifact-id</artifactId>"));

        MavenDependency dependency = new MavenDependency("test.group.id", "test-artifact-id");

        assertTrue(service.addDeployableToCargoRunner(dependency, webappContext));

        String after = contentOf(pomXml);
        assertEquals(1, StringUtils.countMatches(after, "<context>" + webappContext + "</context>"));
        assertEquals(1, StringUtils.countMatches(after, "<groupId>test.group.id</groupId>"));
        assertEquals(1, StringUtils.countMatches(after, "<artifactId>test-artifact-id</artifactId>"));

        assertTrue(service.addDeployableToCargoRunner(dependency, webappContext));

        after = contentOf(pomXml);
        assertEquals(1, StringUtils.countMatches(after, "<context>" + webappContext + "</context>"));
        assertEquals(1, StringUtils.countMatches(after, "<groupId>test.group.id</groupId>"));
        assertEquals(1, StringUtils.countMatches(after, "<artifactId>test-artifact-id</artifactId>"));
    }

    @Test
    public void no_pom_file() throws Exception {
        createModifiableFile("/services/mavencargo/no-cargo-plugin.xml", "dummy.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(MavenModelUtils.class).build()) {
            assertFalse(service.addDeployableToCargoRunner(null, null));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Error parsing pom")));
        }
    }

    @Test
    public void noCargoPlugin() throws Exception {
        createModifiableFile("/services/mavencargo/no-cargo-plugin.xml", "pom.xml");

        assertFalse(service.addDeployableToCargoRunner(null, null));
    }

    @Test
    public void noCargoProfile() throws Exception {
        createModifiableFile("/services/mavencargo/no-cargo-profile.xml", "pom.xml");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(MavenCargoServiceImpl.class).build()) {
            assertFalse(service.addDeployableToCargoRunner(null, null));
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Failed to locate profile 'cargo.run' in project root pom.xml.")));
        }
    }

    @Test
    public void addSharedClasspathTest() throws Exception {
        final String groupId = "org.onehippo.cms";
        final String artifactId ="hippo-plugins-shared";

        File pomXml = createModifiableFile("/project/pom.xml", "pom.xml");

        String before = contentOf(pomXml);
        assertEquals(0, StringUtils.countMatches(before, "<classpath>shared</classpath>"));

        MavenDependency dependency = new MavenDependency(groupId, artifactId);
        assertTrue(service.addDependencyToCargoSharedClasspath(dependency));
        assertTrue(service.addDependencyToCargoSharedClasspath(dependency));

        String after = contentOf(pomXml);
        assertEquals(1, StringUtils.countMatches(after, "<classpath>shared</classpath>"));
    }

    @Test
    public void mergeModelTest() throws IOException, XmlPullParserException {
        final File pomXml = createModifiableFile("/project/pom.xml", "pom.xml");

        URL incomingDefinitions = getClass().getResource("/services/mavencargo/test-pom-overlay.xml");
        assertTrue(service.mergeCargoProfile(incomingDefinitions));

        Model model = MavenModelUtils.readPom(pomXml);
        assertNotNull(model);
        Profile cargoProfile = null;
        for (Profile p : model.getProfiles()) {
            if ("cargo.run".equals(p.getId())) {
                cargoProfile = p;
                break;
            }
        }
        assertNotNull(cargoProfile);
        assertTrue(cargoProfile.getProperties().containsKey("es.tcpPort"));
    }

    @Test
    public void addSystemPropertyTest() throws IOException, XmlPullParserException {
        final File pomXml = createModifiableFile("/project/pom.xml", "pom.xml");

        String before = contentOf(pomXml);
        assertEquals(0, StringUtils.countMatches(before, "<my.test.property>newValue</my.test.property>"));

        assertTrue(service.addSystemProperty("my.test.property", "newValue"));

        String after = contentOf(pomXml);
        assertEquals(1, StringUtils.countMatches(after, "<my.test.property>newValue</my.test.property>"));
    }
}
