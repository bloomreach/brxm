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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JcrServiceImplTest extends BaseRepositoryTest {

    private final JcrService jcrServiceUnderTest = new JcrServiceImpl();

    @Test
    public void import_interpolated_resource() throws Exception {
        Session session = jcrService.createSession();
        Node testNamespace = session.getNode("/hippo:namespaces/testnamespace");
        final Map<String, Object> data = new HashMap<>();
        data.put("namespace", "testnamespace");

        assertTrue(jcrServiceUnderTest.importResource(testNamespace, "/services/jcr/testdocument.xml", data));

        // Make sure the XML was imported
        assertTrue(testNamespace.hasNode("testdocument"));

        // Make sure interpolation took place
        Node nodeTypeNode = testNamespace.getNode("testdocument/hipposysedit:nodetype/hipposysedit:nodetype");
        assertEquals("http://www.onehippo.org/testnamespace/nt/1.0", nodeTypeNode.getProperty("hipposysedit:uri").getString());

        jcrService.destroySession(session);

        // Make sure changes were not saved
        session = jcrService.createSession();
        testNamespace = session.getNode("/hippo:namespaces/testnamespace");
        assertFalse(testNamespace.hasNode("testdocument"));
        jcrService.destroySession(session);
    }

    @Test
    public void import_fails_resource_missing() throws Exception {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrServiceImpl.class).build()) {
            assertFalse(jcrServiceUnderTest.importResource(null, "services/jcr/missing.xml", null));
            assertTrue(interceptor.messages().anyMatch(m -> m.endsWith("Can't read resource 'services/jcr/missing.xml'.")));
        }
    }

    @Test
    public void import_resource_invalid_location() throws Exception {
        Session session = jcrService.createSession();
        Node configNode = session.getNode("/hippo:configuration");
        final Map<String, Object> data = new HashMap<>();
        data.put("namespace", "testnamespace");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrServiceImpl.class).build()) {
            assertFalse(jcrServiceUnderTest.importResource(configNode, "/services/jcr/testdocument.xml", data));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to import resource '/services/jcr/testdocument.xml'.")));
        }

        // Make sure the XML was imported
        assertFalse(configNode.hasNode("testdocument"));

        jcrService.destroySession(session);
    }

    @Test
    public void import_translations_no_resource() throws Exception {
        final Map<String, Object> data = new HashMap<>();
        data.put("prefix", "testnamespace");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrServiceImpl.class).build()) {
            assertFalse(jcrServiceUnderTest.importTranslationsResource(null, "/services/jcr/missing.json", data));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Can't read resource '/services/jcr/missing.json'.")));
        }
    }

    @Test
    public void import_translation_bad_repository() throws Exception {
        Session session = jcrService.createSession();
        session.getNode("/hippo:configuration/hippo:translations").remove();
        session.save();

        final Map<String, Object> data = new HashMap<>();
        data.put("prefix", "testnamespace");
        data.put("document", "foo");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrServiceImpl.class).build()) {
            assertFalse(jcrServiceUnderTest.importTranslationsResource(session, "/services/jcr/translations.json", data));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to import translations '/services/jcr/translations.json'.")));
        }

        jcrService.destroySession(session);
    }

    @Test
    public void import_translations() throws Exception {
        Session session = jcrService.createSession();

        final Map<String, Object> data = new HashMap<>();
        data.put("prefix", "testnamespace");
        data.put("document", "foo");

        assertTrue(jcrServiceUnderTest.importTranslationsResource(session, "/services/jcr/translations.json", data));

        assertTrue(session.nodeExists("/hippo:configuration/hippo:translations/hippo:types/testnamespace:foo/en"));
        Node en = session.getNode("/hippo:configuration/hippo:translations/hippo:types/testnamespace:foo/en");
        assertEquals("Keywords", en.getProperty("testnamespace:tags").getString());

        // ensure service doesn't save
        jcrService.refreshSession(session, false);

        assertFalse(session.nodeExists("/hippo:configuration/hippo:translations/hippo:types"));
        jcrService.destroySession(session);
    }
}
