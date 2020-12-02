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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.TestUserContext;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldsInformation;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.repository.l10n.ResourceBundle;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.UncheckedExecutionException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import static org.powermock.api.easymock.PowerMock.expectPrivate;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({ContentTypeContext.class, DocumentTypesServiceImpl.class, FieldTypeUtils.class,
        LocalizationUtils.class})
public class DocumentTypesServiceImplTest {

    private final DocumentTypesService documentTypesService = DocumentTypesService.get();

    @Before
    public void setup() {
        mockStatic(ContentTypeContext.class);
        mockStatic(FieldTypeUtils.class);
        mockStatic(LocalizationUtils.class);

        // clear document type cache
        documentTypesService.invalidateCache();
    }

    @Test
    public void getDocumentTypeNoContext() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);

        expect(ContentTypeContext.createForDocumentType(id, userContext, docType)).andReturn(Optional.empty());

        replayAll();

        try {
            documentTypesService.getDocumentType(id, userContext);
            fail("No exception");
        } catch (UncheckedExecutionException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof NotFoundException);
            assertNull(((NotFoundException)cause).getPayload());
        }

        verifyAll();
    }

    @Test
    public void getDocumentTypeNotADocument() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);

        expect(ContentTypeContext.createForDocumentType(id, userContext, docType)).andReturn(Optional.of(context));
        expect(context.getContentType()).andReturn(contentType);
        expect(contentType.isDocumentType()).andReturn(false);

        replayAll();

        try {
            documentTypesService.getDocumentType(id, userContext);
            fail("No exception");
        } catch (UncheckedExecutionException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof NotFoundException);
            assertNull(((NotFoundException)cause).getPayload());
        }

        verifyAll();
    }

    @Test
    public void getDocumentTypeNoDisplayName() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final List<FieldType> fields = new ArrayList<>();
        final FieldsInformation fieldsInformation = FieldsInformation.allSupported();

        expect(ContentTypeContext.createForDocumentType(id, userContext, docType)).andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.empty())).andReturn(Optional.empty());
        docType.setId(id);
        expectLastCall();
        expect(docType.getFields()).andReturn(fields);

        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(fieldsInformation);
        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInformation, context);
        expectLastCall();

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.empty());
        expect(contentType.isDocumentType()).andReturn(true);
        expect(contentType.getValidators()).andReturn(Collections.emptyList());

        docType.setAllFieldsIncluded(true);
        expectLastCall();

        docType.setCanCreateAllRequiredFields(true);
        expectLastCall();

        docType.setUnsupportedFieldTypes(Collections.emptySet());
        expectLastCall();

        docType.setUnsupportedRequiredFieldTypes(Collections.emptySet());
        expectLastCall();

        replayAll();

        assertThat(documentTypesService.getDocumentType(id, userContext), equalTo(docType));

        verifyAll();
    }

    @Test
    public void getDocumentTypeWithDisplayName() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final ResourceBundle resourceBundle = createMock(ResourceBundle.class);
        final List<FieldType> fields = new ArrayList<>();
        final FieldsInformation fieldsInformation = FieldsInformation.allSupported();

        expect(ContentTypeContext.createForDocumentType(id, userContext, docType)).andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.of(resourceBundle)))
                .andReturn(Optional.of("Document Display Name"));
        docType.setId(id);
        expectLastCall();

        docType.setDisplayName("Document Display Name");
        expectLastCall();

        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(fieldsInformation);
        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInformation, context);
        expectLastCall();

        docType.setAllFieldsIncluded(true);
        expectLastCall();

        docType.setCanCreateAllRequiredFields(true);
        expectLastCall();

        docType.setUnsupportedFieldTypes(Collections.emptySet());
        expectLastCall();

        docType.setUnsupportedRequiredFieldTypes(Collections.emptySet());
        expectLastCall();

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.of(resourceBundle));
        expect(contentType.isDocumentType()).andReturn(true);
        expect(contentType.getValidators()).andReturn(Collections.emptyList());

        replayAll();

        assertThat(documentTypesService.getDocumentType(id, userContext), equalTo(docType));

        verifyAll();
    }

    @Test
    public void getDocumentTypeWithoutAllFields() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final List<FieldType> fields = new ArrayList<>();
        final FieldsInformation fieldsInfo = new FieldsInformation();
        fieldsInfo.setAllFieldsIncluded(false);
        fieldsInfo.setCanCreateAllRequiredFields(true);
        fieldsInfo.addUnsupportedField("Test");

        expect(ContentTypeContext.createForDocumentType(id, userContext, docType)).andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.empty())).andReturn(Optional.empty());

        docType.setId(id);
        expectLastCall();

        expect(docType.getFields()).andReturn(fields);
        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(fieldsInfo);
        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInfo, context);
        expectLastCall();

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.empty());
        expect(contentType.isDocumentType()).andReturn(true);
        expect(contentType.getValidators()).andReturn(Collections.emptyList());

        docType.setAllFieldsIncluded(false);
        expectLastCall();

        docType.setCanCreateAllRequiredFields(true);
        expectLastCall();

        docType.setUnsupportedFieldTypes(Collections.singleton("Custom"));
        expectLastCall();

        docType.setUnsupportedRequiredFieldTypes(Collections.emptySet());
        expectLastCall();

        replayAll();

        assertThat(documentTypesService.getDocumentType(id, userContext), is(equalTo(docType)));

        verifyAll();
    }

    @Test
    public void getDocumentTypeWithUnsupportedRequiredField() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final List<FieldType> fields = new ArrayList<>();

        expect(ContentTypeContext.createForDocumentType(id, userContext, docType)).andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.empty())).andReturn(Optional.empty());

        docType.setId(id);
        expectLastCall();

        expect(docType.getFields()).andReturn(fields);

        final FieldsInformation fieldsInfo = new FieldsInformation();
        fieldsInfo.addUnsupportedField("Test", Collections.singletonList("required"));

        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(fieldsInfo);
        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInfo, context);
        expectLastCall();

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.empty());
        expect(contentType.isDocumentType()).andReturn(true);
        expect(contentType.getValidators()).andReturn(Collections.emptyList());

        docType.setAllFieldsIncluded(false);
        expectLastCall();

        docType.setCanCreateAllRequiredFields(false);
        expectLastCall();

        docType.setUnsupportedFieldTypes(Collections.singleton("Custom"));
        expectLastCall();

        docType.setUnsupportedRequiredFieldTypes(Collections.singleton("Custom"));
        expectLastCall();

        replayAll();

        assertThat(documentTypesService.getDocumentType(id, userContext), is(equalTo(docType)));

        verifyAll();
    }

    @Test
    public void getDocumentTypeWithValidators() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = PowerMock.createMockAndExpectNew(DocumentType.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final List<FieldType> fields = new ArrayList<>();
        final List<String> validatorNames = Arrays.asList("validator1", "validator2");

        expect(ContentTypeContext.createForDocumentType(id, userContext, docType)).andReturn(Optional.of(context));
        expect(LocalizationUtils.determineDocumentDisplayName(id, Optional.empty())).andReturn(Optional.empty());

        docType.setId(id);
        expectLastCall();

        expect(docType.getFields()).andReturn(fields);

        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();
        expect(FieldTypeUtils.populateFields(fields, context)).andReturn(fieldsInfo);
        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInfo, context);
        expectLastCall();

        expect(context.getContentType()).andReturn(contentType);
        expect(context.getResourceBundle()).andReturn(Optional.empty());
        expect(contentType.isDocumentType()).andReturn(true);
        expect(contentType.getValidators()).andReturn(validatorNames);

        docType.setAllFieldsIncluded(true);
        expectLastCall();

        docType.setCanCreateAllRequiredFields(true);
        expectLastCall();

        docType.setUnsupportedFieldTypes(Collections.emptySet());
        expectLastCall();

        docType.setUnsupportedRequiredFieldTypes(Collections.emptySet());
        expectLastCall();

        docType.addValidatorName("validator1");
        expectLastCall();
        docType.addValidatorName("validator2");
        expectLastCall();

        replayAll();

        assertThat(documentTypesService.getDocumentType(id, userContext), is(equalTo(docType)));

        verifyAll();
    }

    @Test
    public void documentTypeIsCached() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = createMock(DocumentType.class);
        final String method = "createDocumentType";

        final DocumentTypesService docTypesServiceMock = createPartialMock(DocumentTypesServiceImpl.class, method);
        expectPrivate(docTypesServiceMock, method, id, userContext).andReturn(docType);

        replayAll();

        final DocumentType documentType1 = docTypesServiceMock.getDocumentType(id, userContext);
        final DocumentType documentType2 = docTypesServiceMock.getDocumentType(id, userContext);
        assertThat(documentType1, sameInstance(documentType2));

        verifyAll();
    }

    @Test
    public void documentTypeCacheCanBeInvalidated() throws Exception {
        final String id = "document:type";
        final UserContext userContext = new TestUserContext();
        final DocumentType docType = createMock(DocumentType.class);
        final String method = "createDocumentType";

        final DocumentTypesService docTypesServiceMock = createPartialMock(DocumentTypesServiceImpl.class, method);
        expectPrivate(docTypesServiceMock, method, id, userContext).andReturn(docType).times(2);

        replayAll();

        docTypesServiceMock.getDocumentType(id, userContext);
        docTypesServiceMock.invalidateCache();
        docTypesServiceMock.getDocumentType(id, userContext);
        verifyAll();
    }
}
