/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.templatequery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;

import org.easymock.IExpectationSetters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentTypeInfo;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.l10n.ResourceBundle;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeIterator;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({TemplateQueryServiceImpl.class, TemplateQueryUtils.class, LocalizationUtils.class})
public class TemplateQueryServiceImplTest {

    private static final String TEMPLATES_PATH = "/hippo:configuration/hippo:queries/hippo:templates";

    private final TemplateQueryService templateQueryService = TemplateQueryService.get();
    private MockNode root;

    @Before
    public void setup() throws RepositoryException {
        root = MockNode.root();

        PowerMock.mockStatic(LocalizationUtils.class);
        PowerMock.mockStatic(TemplateQueryUtils.class);
    }

    @Test
    public void testTemplateQueryNotFound() throws Exception {
        final String id = "new-document";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");

        try {
            templateQueryService.getDocumentTypeInfos(id, session, locale);
            fail("No exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }
    }

    @Test
    public void testTemplateQueryNotOfTypeQueryNode() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        expectTemplateQueryNode(session, id, "nt:unstructed");

        replayAll(session);

        try {
            templateQueryService.getDocumentTypeInfos(id, session, locale);
            fail("No exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        verifyAll();
    }

    @Test
    public void handleInvalidQueryException() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        expectTemplateQuery(session, id).andThrow(new InvalidQueryException());

        replayAll(session);

        try {
            templateQueryService.getDocumentTypeInfos(id, session, locale);
            fail("No exception");
        } catch (final ErrorWithPayloadException e) {
            assertEquals(INTERNAL_SERVER_ERROR, e.getStatus());
        }

        verifyAll();
    }

    @Test
    public void testTemplateQuery() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        final MockNode prototypeNode = root.addNode("hipposysedit:prototype", "hipposysedit:prototype");
        prototypeNode.setPrimaryType("my:prototype");
        final MockNode prototypeNode2 = root.addNode("prototype2", "hipposysedit:prototype");
        final MockNode prototypeNode3 = root.addNode("prototype3", "hipposysedit:prototype");

        final NodeIterator mockNodes = new MockNodeIterator(Arrays.asList(prototypeNode, prototypeNode2, prototypeNode3));
        expectTemplateQuery(session, id).andReturn(mockNodes);

        expectLocalizedDisplayName(locale, "my:prototype", Optional.of("My prototype"));
        expectLocalizedDisplayName(locale, "prototype2", Optional.of("Prototype 2"));
        expectLocalizedDisplayName(locale, "prototype3", Optional.empty());

        replayAll(session);

        final List<DocumentTypeInfo> documentTypeInfos = templateQueryService.getDocumentTypeInfos(id, session, locale);
        assertEquals(3, documentTypeInfos.size());

        final DocumentTypeInfo info = documentTypeInfos.get(0);
        assertEquals("my:prototype", info.getId());
        assertEquals("My prototype", info.getDisplayName());

        final DocumentTypeInfo info2 = documentTypeInfos.get(1);
        assertEquals("prototype2", info2.getId());
        assertEquals("Prototype 2", info2.getDisplayName());

        final DocumentTypeInfo info3 = documentTypeInfos.get(2);
        assertEquals("prototype3", info3.getId());
        assertEquals("prototype3", info3.getDisplayName());

        verifyAll();
    }

    @Test
    public void testInvalidPrototypes() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        final MockNode prototypeNode = root.addNode("hipposysedit:prototype", "hipposysedit:prototype");
        prototypeNode.setPrimaryType("hipposysedit:document");

        final NodeIterator mockNodes = new MockNodeIterator(Collections.singletonList(prototypeNode));

        expectTemplateQuery(session, id).andReturn(mockNodes);

        replayAll(session);
        final List<DocumentTypeInfo> documentTypeInfos = templateQueryService.getDocumentTypeInfos(id, session, locale);
        assertTrue(documentTypeInfos.isEmpty());

        verifyAll();
    }

    private static MockNode expectTemplateQueryNode(final Session session, final String nodeName, final String nodeType)
            throws RepositoryException {
        final String templateQueryPath = TEMPLATES_PATH + "/" + nodeName;
        final MockNode templateQueryNode = new MockNode(nodeName, nodeType);

        expect(session.nodeExists(templateQueryPath)).andReturn(true);
        expect(session.getNode(templateQueryPath)).andReturn(templateQueryNode);

        return templateQueryNode;
    }

    private static IExpectationSetters<NodeIterator> expectTemplateQuery(final Session session, final String nodeName)
            throws RepositoryException {
        final Node templateQueryNode = expectTemplateQueryNode(session, nodeName, "nt:query");
        return expect(TemplateQueryUtils.executeQuery(session, templateQueryNode));
    }

    private static void expectLocalizedDisplayName(final Locale locale, final String nodeName,
                                                   final Optional<String> displayNameOptional) {
        final Optional<ResourceBundle> optionalResourceBundle = Optional.of(createMock(ResourceBundle.class));
        expect(LocalizationUtils.getResourceBundleForDocument(nodeName, locale)).andReturn(optionalResourceBundle);
        expect(LocalizationUtils.determineDocumentDisplayName(nodeName, optionalResourceBundle))
                .andReturn(displayNameOptional);
    }
}
