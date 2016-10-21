/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HippoServiceRegistry.class, LocalizationUtils.class, NamespaceUtils.class})
public class ContentTypeContextTest {

    @Test
    public void createDocumentTypeContext() throws Exception {
        final ContentTypeService contentTypeService = createMock(ContentTypeService.class);
        final ContentTypes contentTypes = createMock(ContentTypes.class);
        final ContentType contentType = createMock(ContentType.class);
        final Session session = createMock(Session.class);
        final Node rootNode = createMock(Node.class);
        final Locale locale = new Locale("en");

        PowerMock.mockStaticPartial(HippoServiceRegistry.class, "getService");
        PowerMock.mockStaticPartial(LocalizationUtils.class, "getResourceBundleForDocument");
        PowerMock.mockStaticPartial(NamespaceUtils.class, "getDocumentTypeRootNode");

        expect(HippoServiceRegistry.getService(anyObject())).andReturn(contentTypeService);
        expect(contentTypeService.getContentTypes()).andReturn(contentTypes);
        expect(contentTypes.getType("type")).andReturn(contentType);
        expect(LocalizationUtils.getResourceBundleForDocument("type", locale)).andReturn(Optional.empty());
        expect(NamespaceUtils.getDocumentTypeRootNode("type", session)).andReturn(Optional.of(rootNode));

        replay(contentTypeService, contentTypes);
        PowerMock.replayAll();

        final ContentTypeContext context = ContentTypeContext.createDocumentTypeContext("type", session, locale, 2);

        assertThat(context.getContentType(), equalTo(contentType));
        assertThat(context.getContentTypeRoot(), equalTo(rootNode));
        assertThat(context.getResourceBundle().isPresent(), equalTo(false));
        assertThat(context.getLevel(), equalTo(2));
    }

    @Test(expected = ContentTypeException.class)
    public void createDocumentTypeContextWithRepositoryException() throws Exception {
        final ContentTypeService contentTypeService = createMock(ContentTypeService.class);

        PowerMock.mockStaticPartial(HippoServiceRegistry.class, "getService");

        expect(HippoServiceRegistry.getService(anyObject())).andReturn(contentTypeService);
        expect(contentTypeService.getContentTypes()).andThrow(new RepositoryException());

        replay(contentTypeService);
        PowerMock.replayAll();

        ContentTypeContext.createDocumentTypeContext("type", null, null, 0);
    }

    @Test(expected = ContentTypeException.class)
    public void createDocumentTypeContextWithMissingDocumentTypeRoot() throws Exception {
        final ContentTypeService contentTypeService = createMock(ContentTypeService.class);
        final ContentTypes contentTypes = createMock(ContentTypes.class);
        final ContentType contentType = createMock(ContentType.class);
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");

        PowerMock.mockStaticPartial(HippoServiceRegistry.class, "getService");
        PowerMock.mockStaticPartial(LocalizationUtils.class, "getResourceBundleForDocument");
        PowerMock.mockStaticPartial(NamespaceUtils.class, "getDocumentTypeRootNode");

        expect(HippoServiceRegistry.getService(anyObject())).andReturn(contentTypeService);
        expect(contentTypeService.getContentTypes()).andReturn(contentTypes);
        expect(contentTypes.getType("type")).andReturn(contentType);
        expect(LocalizationUtils.getResourceBundleForDocument("type", locale)).andReturn(Optional.empty());
        expect(NamespaceUtils.getDocumentTypeRootNode("type", session)).andReturn(Optional.empty());

        replay(contentTypeService, contentTypes);
        PowerMock.replayAll();

        ContentTypeContext.createDocumentTypeContext("type", session, locale, 0);
    }
}
