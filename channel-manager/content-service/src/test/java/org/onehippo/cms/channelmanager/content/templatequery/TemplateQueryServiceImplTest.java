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

import java.util.ArrayList;
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
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.l10n.ResourceBundle;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeIterator;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.INVALID_TEMPLATE_QUERY;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.TEMPLATE_QUERY_NOT_FOUND;
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
            templateQueryService.getTemplateQuery(id, session, locale);
            fail("No exception");
        } catch (InternalServerErrorException e) {
            assertErrorPayload(e, TEMPLATE_QUERY_NOT_FOUND, "templateQuery", "new-document");
        }
    }

    @Test
    public void testTemplateQueryNotOfTypeQueryNode() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        expectTemplateQueryNode(session, id, "nt:unstructured");

        replayAll(session);

        try {
            templateQueryService.getTemplateQuery(id, session, locale);
            fail("No exception");
        } catch (InternalServerErrorException e) {
            assertErrorPayload(e, TEMPLATE_QUERY_NOT_FOUND, "templateQuery", "new-document");
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
            templateQueryService.getTemplateQuery(id, session, locale);
            fail("No exception");
        } catch (final InternalServerErrorException e) {
            assertErrorPayload(e, INVALID_TEMPLATE_QUERY, "templateQuery", "new-document");
        }

        verifyAll();
    }

    @Test
    public void testTemplateQueryWithDisplayName() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        final MockNode prototypeNode1 = root.addNode("hipposysedit:prototype", "hipposysedit:prototype");
        prototypeNode1.setPrimaryType("my:prototype");
        final MockNode prototypeNode2 = root.addNode("prototype2", "hipposysedit:prototype");

        final NodeIterator mockNodes = new MockNodeIterator(Arrays.asList(prototypeNode1, prototypeNode2));
        expectTemplateQuery(session, id).andReturn(mockNodes);

        expectLocalizedDisplayName(locale, "my:prototype", Optional.of("My prototype"));
        expectLocalizedDisplayName(locale, "prototype2", Optional.of("Prototype 2"));

        replayAll(session);

        final TemplateQuery templateQuery = templateQueryService.getTemplateQuery(id, session, locale);
        final List<DocumentTypeInfo> documentTypes = templateQuery.getDocumentTypes();
        assertEquals(2, documentTypes.size());

        final DocumentTypeInfo info1 = documentTypes.get(0);
        assertEquals("my:prototype", info1.getId());
        assertEquals("My prototype", info1.getDisplayName());

        final DocumentTypeInfo info2 = documentTypes.get(1);
        assertEquals("prototype2", info2.getId());
        assertEquals("Prototype 2", info2.getDisplayName());

        verifyAll();
    }

    @Test
    public void testTemplateQueryWithoutDisplayName() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        final MockNode prototypeNode1 = root.addNode("hipposysedit:prototype", "hipposysedit:prototype");
        prototypeNode1.setPrimaryType("my:prototype");
        final MockNode prototypeNode2 = root.addNode("prototype2", "hipposysedit:prototype");

        final NodeIterator mockNodes = new MockNodeIterator(Arrays.asList(prototypeNode1, prototypeNode2));
        expectTemplateQuery(session, id).andReturn(mockNodes);

        expectLocalizedDisplayName(locale, "my:prototype", Optional.empty());
        expectLocalizedDisplayName(locale, "prototype2", Optional.empty());

        replayAll(session);

        final TemplateQuery templateQuery = templateQueryService.getTemplateQuery(id, session, locale);
        final List<DocumentTypeInfo> documentTypes = templateQuery.getDocumentTypes();
        assertEquals(2, documentTypes.size());

        final DocumentTypeInfo info1 = documentTypes.get(0);
        assertEquals("my:prototype", info1.getId());
        assertEquals("my:prototype", info1.getDisplayName());

        final DocumentTypeInfo info2 = documentTypes.get(1);
        assertEquals("prototype2", info2.getId());
        assertEquals("prototype2", info2.getDisplayName());

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
        final TemplateQuery templateQuery = templateQueryService.getTemplateQuery(id, session, locale);
        final List<DocumentTypeInfo> documentTypes = templateQuery.getDocumentTypes();
        assertTrue(documentTypes.isEmpty());

        verifyAll();
    }

    @Test
    public void testTemplateQueryIsSorted() throws Exception {
        final String id = "new-document";
        final Locale locale = new Locale("en");
        final Session session = createMock(Session.class);

        expectTemplateQueryToReturn(id, session, locale, "pêche", "Péché", "peach");

        replayAll(session);

        final TemplateQuery templateQuery = templateQueryService.getTemplateQuery(id, session, locale);
        final List<DocumentTypeInfo> documentTypes = templateQuery.getDocumentTypes();
        assertEquals(3, documentTypes.size());

        assertEquals("peach", documentTypes.get(0).getDisplayName());
        assertEquals("Péché", documentTypes.get(1).getDisplayName());
        assertEquals("pêche", documentTypes.get(2).getDisplayName());

        verifyAll();
    }

    /**
     * To validate locale sorting, we use the French locale.
     * See <a href="https://docs.oracle.com/javase/tutorial/i18n/text/locale.html"></a> for more info.
     */
    @Test
    public void testTemplateQueryIsSortedForLocale() throws Exception {
        final String id = "new-document";
        final Locale locale = Locale.FRANCE;
        final Session session = createMock(Session.class);

        expectTemplateQueryToReturn(id, session, locale, "pêche", "Péché", "peach");

        replayAll(session);

        final TemplateQuery templateQuery = templateQueryService.getTemplateQuery(id, session, locale);
        final List<DocumentTypeInfo> documentTypes = templateQuery.getDocumentTypes();
        assertEquals(3, documentTypes.size());

        assertEquals("peach", documentTypes.get(0).getDisplayName());
        assertEquals("pêche", documentTypes.get(1).getDisplayName());
        assertEquals("Péché", documentTypes.get(2).getDisplayName());

        verifyAll();
    }

    private void assertErrorPayload(final InternalServerErrorException e, final ErrorInfo.Reason reason,
                                    final String key, final String value) {
        final Object payload = e.getPayload();
        assertTrue(payload instanceof ErrorInfo);

        final ErrorInfo errorInfo = (ErrorInfo) payload;
        assertEquals(reason, errorInfo.getReason());
        assertEquals(1, errorInfo.getParams().size());
        assertEquals(value, errorInfo.getParams().get(key));

    }

    private void expectTemplateQueryToReturn(final String id, final Session session, final Locale locale, final String... displayNames) throws RepositoryException {
        final List<MockNode> nodes = new ArrayList<>(displayNames.length);
        for (int i = 0; i < displayNames.length; i++) {
            final String prototypeName = "prototype" + i;
            nodes.add(root.addNode(prototypeName, "hipposysedit:prototype"));
            expectLocalizedDisplayName(locale, prototypeName, Optional.of(displayNames[i]));
        }

        expectTemplateQuery(session, id).andReturn(new MockNodeIterator(nodes));
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
