/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.repository.l10n.ResourceBundle;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import static org.powermock.api.easymock.PowerMock.expectPrivate;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({ContentTypeContext.class, DocumentTypesServiceImpl.class, FieldTypeUtils.class,
        LocalizationUtils.class})
public class DocumentTypesServiceImplTest {

    private DocumentTypesService documentTypesService = DocumentTypesService.get();

    @Before
    public void setup() {
        PowerMock.mockStatic(ContentTypeContext.class);
        PowerMock.mockStatic(FieldTypeUtils.class);
        PowerMock.mockStatic(LocalizationUtils.class);

        // clear document type cache
        documentTypesService.invalidateCache();
    }

    @Test
    public void getDocumentTypeNoContext() throws Exception {
        final String id = "document:type";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);

        expect(ContentTypeContext.createForDocumentType(id, session, locale, docType))
                .andReturn(Optional.empty());

        replayAll();

        try {
            documentTypesService.getDocumentType(id, session, locale);
            fail("No exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        verifyAll();
    }

    @Test
    public void getDocumentTypeNotADocument() throws Exception {
        final String id = "document:type";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);

        expect(ContentTypeContext.createForDocumentType(id, session, locale, docType))
                .andReturn(Optional.of(context));

        expect(context.getContentType()).andReturn(contentType);
        expect(contentType.isDocumentType()).andReturn(false);

        replayAll();
        replay(context, contentType);

        try {
            documentTypesService.getDocumentType(id, session, locale);
            fail("No exception");
        } catch (NotFoundException e) {
            assertNull(e.getPayload());
        }

        verify(context, contentType);
        verifyAll();
    }

    @Test
    public void getDocumentTypeNoDisplayName() throws Exception {
        final String id = "document:type";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final List<FieldType> fields = new ArrayList<>();

        expect(ContentTypeContext.createForDocumentType(id, session, locale, docType))
                .andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.empty())).andReturn(Optional.empty());
        docType.setId(id);
        expectLastCall();
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(true);

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.empty());
        expect(contentType.isDocumentType()).andReturn(true);

        docType.setAllFieldsIncluded(true);
        expectLastCall();

        replayAll();
        replay(context, contentType);

        assertThat(documentTypesService.getDocumentType(id, session, locale), equalTo(docType));

        verify(context, contentType);
        verifyAll();
    }

    @Test
    public void getDocumentTypeWithDisplayName() throws Exception {
        final String id = "document:type";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);
        final List<FieldType> fields = new ArrayList<>();

        expect(ContentTypeContext.createForDocumentType(id, session, locale, docType))
                .andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.of(resourceBundle)))
                .andReturn(Optional.of("Document Display Name"));
        docType.setId(id);
        expectLastCall();
        docType.setDisplayName("Document Display Name");
        expectLastCall();
        expect(docType.getFields()).andReturn(fields);

        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(true);

        docType.setAllFieldsIncluded(true);
        expectLastCall();

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.of(resourceBundle));
        expect(contentType.isDocumentType()).andReturn(true);

        replayAll();
        replay(context, contentType);

        assertThat(documentTypesService.getDocumentType(id, session, locale), equalTo(docType));

        verify(context, contentType);
        verifyAll();
    }

    @Test
    public void getDocumentTypeWithoutAllFields() throws Exception {
        final String id = "document:type";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final List<FieldType> fields = new ArrayList<>();

        expect(ContentTypeContext.createForDocumentType(id, session, locale, docType))
                .andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.empty())).andReturn(Optional.empty());
        docType.setId(id);
        expectLastCall();
        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(false);

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.empty());
        expect(contentType.isDocumentType()).andReturn(true);

        docType.setAllFieldsIncluded(false);
        expectLastCall();

        replayAll();
        replay(context, contentType);

        assertThat(documentTypesService.getDocumentType(id, session, locale), is(equalTo(docType)));

        verify(context, contentType);
        verifyAll();
    }

    @Test
    public void documentTypeIsCached() throws Exception {
        final String id = "document:type";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final DocumentType docType = PowerMock.createMock(DocumentType.class);
        final String method = "createDocumentType";

        final DocumentTypesService docTypesServiceMock = createPartialMock(DocumentTypesServiceImpl.class, method);
        expectPrivate(docTypesServiceMock, method, id, session, locale).andReturn(docType);

        replayAll();

        final DocumentType documentType1 = docTypesServiceMock.getDocumentType(id, session, locale);
        final DocumentType documentType2 = docTypesServiceMock.getDocumentType(id, session, locale);
        assertThat(documentType1, is(equalTo(documentType2)));

        verifyAll();
    }

    @Test
    public void documentTypeCacheCanBeInvalidated() throws Exception {
        final String id = "document:type";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final DocumentType docType = PowerMock.createMock(DocumentType.class);
        final String method = "createDocumentType";

        final DocumentTypesService docTypesServiceMock = createPartialMock(DocumentTypesServiceImpl.class, method);
        expectPrivate(docTypesServiceMock, method, id, session, locale).andReturn(docType).times(2);

        replayAll();

        docTypesServiceMock.getDocumentType(id, session, locale);
        docTypesServiceMock.invalidateCache();
        docTypesServiceMock.getDocumentType(id, session, locale);
        verifyAll();
    }
}
