/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.TestUserContext;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.repository.l10n.ResourceBundle;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({ContentTypeContext.class, HippoServiceRegistry.class, LocalizationUtils.class, NamespaceUtils.class})
public class ContentTypeContextTest {

    private UserContext userContext;

    @Before
    public void setup() {
        PowerMock.mockStatic(HippoServiceRegistry.class);
        PowerMock.mockStatic(LocalizationUtils.class);
        PowerMock.mockStatic(NamespaceUtils.class);

        userContext = new TestUserContext();
    }

    @Test
    public void getContentTypeWithRepositoryException() throws Exception {
        final ContentTypeService contentTypeService = createMock(ContentTypeService.class);

        expect(HippoServiceRegistry.getService(ContentTypeService.class)).andReturn(contentTypeService);

        expect(contentTypeService.getContentTypes()).andThrow(new RepositoryException());

        PowerMock.replayAll();
        replay(contentTypeService);

        assertFalse(ContentTypeContext.getContentType("namespaced:type").isPresent());

        verify(contentTypeService);
        PowerMock.verifyAll();
    }

    @Test
    public void getContentTypeNull() throws Exception {
        final ContentTypeService contentTypeService = createMock(ContentTypeService.class);
        final ContentTypes contentTypes = createMock(ContentTypes.class);

        expect(HippoServiceRegistry.getService(ContentTypeService.class)).andReturn(contentTypeService);

        expect(contentTypeService.getContentTypes()).andReturn(contentTypes);
        expect(contentTypes.getType("namespaced:type")).andReturn(null);

        PowerMock.replayAll();
        replay(contentTypeService, contentTypes);

        assertFalse(ContentTypeContext.getContentType("namespaced:type").isPresent());

        verify(contentTypeService, contentTypes);
        PowerMock.verifyAll();
    }

    @Test
    public void getContentType() throws Exception {
        final ContentType contentType = provideContentType("namespaced:type");

        PowerMock.replayAll();

        assertThat(ContentTypeContext.getContentType("namespaced:type").get(), equalTo(contentType));

        PowerMock.verifyAll();
    }

    private ContentType provideContentType(final String id) throws Exception {
        final ContentTypeService contentTypeService = createMock(ContentTypeService.class);
        final ContentTypes contentTypes = createMock(ContentTypes.class);
        final ContentType contentType = createMock(ContentType.class);

        expect(HippoServiceRegistry.getService(ContentTypeService.class)).andReturn(contentTypeService);
        expect(contentTypeService.getContentTypes()).andReturn(contentTypes);
        expect(contentTypes.getType(id)).andReturn(contentType);

        replay(contentTypeService, contentTypes);

        return contentType;
    }

    @Test
    public void createForDocumentTypeWithRepositoryException() throws Exception {
        final ContentTypeService contentTypeService = createMock(ContentTypeService.class);
        final String id = "namespaced:type";
        final DocumentType docType = new DocumentType();

        expect(HippoServiceRegistry.getService(ContentTypeService.class)).andReturn(contentTypeService);

        expect(contentTypeService.getContentTypes()).andThrow(new RepositoryException());

        PowerMock.replayAll();
        replay(contentTypeService);

        assertFalse(ContentTypeContext.createForDocumentType(id, userContext, docType).isPresent());

        verify(contentTypeService);
        PowerMock.verifyAll();
    }

    @Test
    public void createForDocumentTypeWithoutContentTypeRootNode() throws Exception {
        final String id = "namespaced:type";
        final DocumentType docType = new DocumentType();

        expect(NamespaceUtils.getContentTypeRootNode(id, userContext.getSession())).andReturn(Optional.empty());
        provideContentType(id);

        PowerMock.replayAll();

        assertFalse(ContentTypeContext.createForDocumentType(id, userContext, docType).isPresent());

        PowerMock.verifyAll();
    }

    @Test
    public void createForDocumentType() throws Exception {
        final String id = "namespaced:type";
        final Session session = userContext.getSession();
        final Locale locale = userContext.getLocale();
        final DocumentType docType = new DocumentType();
        final ContentType contentType = provideContentType(id);
        final Node contentTypeRootNode = createMock(Node.class);
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        expect(NamespaceUtils.getContentTypeRootNode(id, session)).andReturn(Optional.of(contentTypeRootNode)).anyTimes();
        expect(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, true)).andReturn(Optional.empty());
        expect(LocalizationUtils.getResourceBundleForDocument(id, locale)).andReturn(Optional.of(resourceBundle));

        expect(contentType.getSuperTypes()).andReturn(Collections.emptySortedSet());

        PowerMock.replayAll();
        replay(contentType);

        final ContentTypeContext context = ContentTypeContext.createForDocumentType(id, userContext,docType).get();
        assertThat(context.getContentType(), equalTo(contentType));
        assertThat(context.getSession(), equalTo(session));
        assertThat(context.getContentTypeRoot(), equalTo(contentTypeRootNode));
        assertThat(context.getLocale(), equalTo(locale));
        assertThat(context.getDocumentType(), equalTo(docType));
        assertThat(context.getLevel(), equalTo(0));
        assertThat(context.getResourceBundle().get(), equalTo(resourceBundle));
        assertTrue(context.getFieldScanningContexts().isEmpty());

        verify(contentType);
        PowerMock.verifyAll();
    }

    @Test
    public void createForDocumentTypeWithSupertypesAndNoResourceBundle() {
        final String id = "namespaced:type";
        final Session session = userContext.getSession();
        final Locale locale = userContext.getLocale();
        final DocumentType docType = new DocumentType();
        final ContentType contentType = createMock(ContentType.class);
        final ContentType superType2 = createMock(ContentType.class);
        final ContentType superType3 = createMock(ContentType.class);
        final ContentType superType4 = createMock(ContentType.class);
        final Node contentTypeRootNode = createMock(Node.class);
        final Node superType3RootNode = createMock(Node.class);
        final Node superType4RootNode = createMock(Node.class);
        final Node mainNodeTypeNode = createMock(Node.class);
        final Node superType4NodeTypeNode = createMock(Node.class);
        final SortedSet<String> superTypes = new TreeSet<>();
        superTypes.add("superType1");
        superTypes.add("superType2");
        superTypes.add("superType3");
        superTypes.add("superType4");

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        // supertype 1 has no content type
        expect(ContentTypeContext.getContentType(id)).andReturn(Optional.of(contentType));
        expect(ContentTypeContext.getContentType("superType1")).andReturn(Optional.empty());
        expect(ContentTypeContext.getContentType("superType2")).andReturn(Optional.of(superType2));
        expect(ContentTypeContext.getContentType("superType3")).andReturn(Optional.of(superType3));
        expect(ContentTypeContext.getContentType("superType4")).andReturn(Optional.of(superType4));

        // supertype 2 has no root node
        expect(NamespaceUtils.getContentTypeRootNode(id, userContext.getSession()))
                .andReturn(Optional.of(contentTypeRootNode)).anyTimes();
        expect(NamespaceUtils.getContentTypeRootNode("superType2", session)).andReturn(Optional.empty());
        expect(NamespaceUtils.getContentTypeRootNode("superType3", session)).andReturn(Optional.of(superType3RootNode));
        expect(NamespaceUtils.getContentTypeRootNode("superType4", session)).andReturn(Optional.of(superType4RootNode));

        // supertype 3 has no node type node
        expect(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, true)).andReturn(Optional.of(mainNodeTypeNode));
        expect(NamespaceUtils.getNodeTypeNode(superType3RootNode, false)).andReturn(Optional.empty());
        expect(NamespaceUtils.getNodeTypeNode(superType4RootNode, false)).andReturn(Optional.of(superType4NodeTypeNode));

        expect(LocalizationUtils.getResourceBundleForDocument(id, locale)).andReturn(Optional.empty());

        expect(contentType.getSuperTypes()).andReturn(superTypes);

        PowerMock.replayAll();
        replay(contentType);

        final ContentTypeContext context = ContentTypeContext.createForDocumentType(id, userContext, docType).get();
        assertThat(context.getContentType(), equalTo(contentType));
        assertThat(context.getSession(), equalTo(session));
        assertThat(context.getContentTypeRoot(), equalTo(contentTypeRootNode));
        assertThat(context.getLocale(), equalTo(locale));
        assertThat(context.getDocumentType(), equalTo(docType));
        assertThat(context.getLevel(), equalTo(0));
        assertFalse(context.getResourceBundle().isPresent());
        assertThat(context.getFieldScanningContexts().size(), equalTo(2));
        assertThat(context.getFieldScanningContexts().get(0).getContentType(), equalTo(contentType));
        assertThat(context.getFieldScanningContexts().get(0).getNodeTypeNode(), equalTo(mainNodeTypeNode));
        assertThat(context.getFieldScanningContexts().get(1).getContentType(), equalTo(superType4));
        assertThat(context.getFieldScanningContexts().get(1).getNodeTypeNode(), equalTo(superType4NodeTypeNode));

        verify(contentType);
        PowerMock.verifyAll();
    }

    @Test
    public void createFromParentNestingTooDeep() throws Exception {
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);

        expect(parentContext.getLevel()).andReturn(9);

        replay(parentContext);

        assertFalse(ContentTypeContext.createFromParent("namespaced:type", parentContext).isPresent());

        verify(parentContext);
    }

    @Test
    public void createFromParent() throws Exception {
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final String id = "namespaced:type";
        final Session session = userContext.getSession();
        final Locale locale = userContext.getLocale();
        final DocumentType docType = new DocumentType();
        final ContentType contentType = createMock(ContentType.class);
        final Node contentTypeRootNode = createMock(Node.class);
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        expect(ContentTypeContext.getContentType(id)).andReturn(Optional.of(contentType));
        expect(NamespaceUtils.getContentTypeRootNode(id, session)).andReturn(Optional.of(contentTypeRootNode)).anyTimes();
        expect(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, true)).andReturn(Optional.empty());
        expect(LocalizationUtils.getResourceBundleForDocument(id, locale)).andReturn(Optional.of(resourceBundle));

        expect(parentContext.getLevel()).andReturn(4);
        expect(parentContext.getUserContext()).andReturn(userContext);
        expect(parentContext.getDocumentType()).andReturn(docType);
        expect(contentType.getSuperTypes()).andReturn(Collections.emptySortedSet());

        PowerMock.replayAll();
        replay(parentContext, contentType);

        final ContentTypeContext context = ContentTypeContext.createFromParent(id, parentContext).get();
        assertThat(context.getContentType(), equalTo(contentType));
        assertThat(context.getSession(), equalTo(session));
        assertThat(context.getContentTypeRoot(), equalTo(contentTypeRootNode));
        assertThat(context.getLocale(), equalTo(locale));
        assertThat(context.getDocumentType(), equalTo(docType));
        assertThat(context.getLevel(), equalTo(5));
        assertThat(context.getResourceBundle().get(), equalTo(resourceBundle));
        assertTrue(context.getFieldScanningContexts().isEmpty());

        verify(parentContext, contentType);
        PowerMock.verifyAll();
    }
}
