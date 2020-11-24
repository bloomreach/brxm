/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.AlwaysBadTestValidator;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.AlwaysGoodTestValidator;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.TestValidatorInstance;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.services.validation.api.ValueContext;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.newCapture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest(FieldTypeUtils.class)
public class ValidationUtilsTest {

    @Before
    public void setUp() {
        mockStatic(FieldTypeUtils.class);
    }

    @Test
    public void validateDocumentWithoutViolations() {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        final Node draftNode = createMock(Node.class);
        final UserContext userContext = new UserContext(null, null, null);

        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), isA(CompoundContext.class))).andReturn(0);

        replayAll();

        final boolean valid = ValidationUtils.validateDocument(document, docType, draftNode, userContext);
        assertTrue(valid);
        assertThat(document.getInfo().getErrorCount(), equalTo(0));

        verifyAll();
    }

    @Test
    public void validateDocumentCreatesCorrectDocumentContext() {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        final Node draftNode = createMock(Node.class);
        final Locale locale = new Locale("en");
        final TimeZone timeZone = TimeZone.getDefault();
        final UserContext userContext = new UserContext(null, locale, timeZone);

        final Capture<CompoundContext> context = newCapture();
        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), capture(context))).andReturn(0);

        replayAll();

        ValidationUtils.validateDocument(document, docType, draftNode, userContext);
        final CompoundContext documentContext = context.getValue();

        assertThat(documentContext.getNode(), equalTo(draftNode));
        assertThat(documentContext.getLocale(), equalTo(locale));
        assertThat(documentContext.getTimeZone(), equalTo(timeZone));

        verifyAll();
    }

    @Test
    public void validateDocumentWithFieldViolation() {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        final Node draftNode = createMock(Node.class);
        final UserContext userContext = new UserContext(null, null, null);

        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), isA(CompoundContext.class))).andReturn(1);

        replayAll();

        final boolean valid = ValidationUtils.validateDocument(document, docType, draftNode, userContext);
        assertFalse(valid);
        assertThat(document.getInfo().getErrorCount(), equalTo(1));

        verifyAll();
    }

    @Test
    public void validateDocumentCreatesCorrectTypeContext() throws RepositoryException {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        docType.setId("myproject:newsdocument");
        docType.addValidatorName("documentvalidator");

        final Node handle = MockNode.root();
        final Node draft = handle.addNode("example", "myproject:newsdocument");

        final Locale locale = new Locale("en");
        final TimeZone timeZone = TimeZone.getDefault();
        final UserContext userContext = new UserContext(null, locale, timeZone);

        final ValidatorInstance validator = createMock(ValidatorInstance.class);
        final Capture<ValueContext> context = newCapture();

        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), isA(CompoundContext.class))).andReturn(0);
        expect(FieldTypeUtils.getValidator(eq("documentvalidator"))).andReturn(validator);
        expect(validator.validate(capture(context), eq(draft))).andReturn(Optional.empty());

        replayAll();

        ValidationUtils.validateDocument(document, docType, draft, userContext);
        final ValueContext typeContext = context.getValue();

        assertThat(typeContext.getJcrName(), equalTo("example"));
        assertThat(typeContext.getJcrType(), equalTo("myproject:newsdocument"));
        assertThat(typeContext.getType(), equalTo("myproject:newsdocument"));
        assertThat(typeContext.getDocumentNode(), equalTo(draft));
        assertThat(typeContext.getParentNode(), equalTo(handle));
        assertThat(typeContext.getLocale(), equalTo(locale));
        assertThat(typeContext.getTimeZone(), equalTo(timeZone));

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void validateDocumentCannotCreateTypeContext() throws RepositoryException {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        docType.addValidatorName("documentvalidator");

        final Node draftNode = createMock(Node.class);
        final UserContext userContext = new UserContext(null, null, null);

        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), isA(CompoundContext.class))).andReturn(0);
        expect(draftNode.getName()).andThrow(new RepositoryException());

        replayAll();

        ValidationUtils.validateDocument(document, docType, draftNode, userContext);
    }

    @Test
    public void validateDocumentWithFieldAndTypeViolations() throws RepositoryException {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        docType.setId("myproject:newsdocument");
        docType.addValidatorName("documentvalidator");

        final Node handle = MockNode.root();
        final Node draft = handle.addNode("example", "myproject:newsdocument");
        final UserContext userContext = new UserContext(null, null, null);

        final ValidatorInstance validator = createMock(ValidatorInstance.class);

        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), isA(CompoundContext.class))).andReturn(2);
        expect(FieldTypeUtils.getValidator(eq("documentvalidator"))).andReturn(validator);
        expect(validator.validate(isA(ValueContext.class), eq(draft))).andReturn(Optional.of(() -> "error in document"));

        replayAll();

        final boolean valid = ValidationUtils.validateDocument(document, docType, draft, userContext);
        assertFalse(valid);
        assertThat(document.getInfo().getErrorCount(), equalTo(3));
        assertThat(document.getInfo().getErrorMessages(), equalTo(Collections.singletonList("error in document")));

        verifyAll();
    }

    @Test
    public void validateDocumentIgnoresUnknownDocumentValidators() throws RepositoryException {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        docType.setId("myproject:newsdocument");
        docType.addValidatorName("unknown");
        docType.addValidatorName("documentvalidator");

        final Node handle = MockNode.root();
        final Node draft = handle.addNode("example", "myproject:newsdocument");
        final UserContext userContext = new UserContext(null, null,  null);
        final ValidatorInstance validator = createMock(ValidatorInstance.class);

        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), isA(CompoundContext.class))).andReturn(0);
        expect(FieldTypeUtils.getValidator(eq("unknown"))).andReturn(null);
        expect(FieldTypeUtils.getValidator(eq("documentvalidator"))).andReturn(validator);
        expect(validator.validate(isA(ValueContext.class), eq(draft))).andReturn(Optional.of(() -> "error in document"));

        replayAll();

        final boolean valid = ValidationUtils.validateDocument(document, docType, draft, userContext);
        assertFalse(valid);
        assertThat(document.getInfo().getErrorCount(), equalTo(1));
        assertThat(document.getInfo().getErrorMessages(), equalTo(Collections.singletonList("error in document")));

        verifyAll();
    }

    @Test
    public void validateDocumentIgnoresThrowingDocumentValidators() throws RepositoryException {
        final Document document = new Document();
        final DocumentType docType = new DocumentType();
        docType.setId("myproject:newsdocument");
        docType.addValidatorName("bad");
        docType.addValidatorName("good");

        final Node handle = MockNode.root();
        final Node draft = handle.addNode("example", "myproject:newsdocument");
        final UserContext userContext = new UserContext(null, null, null);

        final ValidatorInstance badValidator = createMock(ValidatorInstance.class);
        final ValidatorInstance goodValidator = createMock(ValidatorInstance.class);

        expect(FieldTypeUtils.validateFieldValues(eq(document.getFields()), eq(docType.getFields()), isA(CompoundContext.class))).andReturn(0);
        expect(FieldTypeUtils.getValidator(eq("bad"))).andReturn(badValidator);
        expect(badValidator.validate(isA(ValueContext.class), eq(draft))).andThrow(new RuntimeException());
        expect(FieldTypeUtils.getValidator(eq("good"))).andReturn(goodValidator);
        expect(goodValidator.validate(isA(ValueContext.class), eq(draft))).andReturn(Optional.of(() -> "error in document"));

        replayAll();

        final boolean valid = ValidationUtils.validateDocument(document, docType, draft, userContext);
        assertFalse(valid);
        assertThat(document.getInfo().getErrorCount(), equalTo(1));
        assertThat(document.getInfo().getErrorMessages(), equalTo(Collections.singletonList("error in document")));

        verifyAll();
    }

    @Test
    public void validateValueWithoutValidators() {
        final FieldValue fieldValue = new FieldValue();
        final ValueContext context = createMock(ValueContext.class);

        final int violations = ValidationUtils.validateValue(fieldValue, context, Collections.emptySet(), "data");

        assertThat(violations, equalTo(0));
    }

    @Test
    public void validateValueOk() {
        final FieldValue fieldValue = new FieldValue();
        final ValueContext context = createMock(ValueContext.class);
        final ValidatorInstance validator = new TestValidatorInstance(new AlwaysGoodTestValidator());

        expect(FieldTypeUtils.getValidator("validatorName")).andReturn(validator);
        replayAll();

        final int violations = ValidationUtils.validateValue(fieldValue, context, Collections.singleton("validatorName"), "data");
        assertThat(violations, equalTo(0));
        verifyAll();
    }

    @Test
    public void validateValueBad() {
        final FieldValue fieldValue = new FieldValue();
        final ValueContext context = createMock(ValueContext.class);
        final ValidatorInstance validator = new TestValidatorInstance(new AlwaysBadTestValidator());

        expect(FieldTypeUtils.getValidator("validatorName")).andReturn(validator);
        replayAll();

        final int violations = ValidationUtils.validateValue(fieldValue, context, Collections.singleton("validatorName"), "data");
        assertThat(violations, equalTo(1));
        assertThat(fieldValue.getErrorInfo().getValidation(), equalTo("validatorName"));
        assertThat(fieldValue.getErrorInfo().getMessage(), equalTo("Always bad"));
        verifyAll();
    }

    @Test
    public void validateValueFirstValidatorBad() {
        final FieldValue fieldValue = new FieldValue();
        final ValueContext context = createMock(ValueContext.class);
        final ValidatorInstance firstValidator = new TestValidatorInstance(new AlwaysBadTestValidator());
        final LinkedHashSet<String> validators = new LinkedHashSet<>(Arrays.asList("first", "second"));

        expect(FieldTypeUtils.getValidator("first")).andReturn(firstValidator);
        replayAll();

        final int violations = ValidationUtils.validateValue(fieldValue, context, validators, "data");
        assertThat(violations, equalTo(1));
        assertThat(fieldValue.getErrorInfo().getValidation(), equalTo("first"));
        assertThat(fieldValue.getErrorInfo().getMessage(), equalTo("Always bad"));
        verifyAll();
    }

    @Test
    public void validateValueSecondValidatorBad() {
        final FieldValue fieldValue = new FieldValue();
        final ValueContext context = createMock(ValueContext.class);
        final ValidatorInstance firstValidator = new TestValidatorInstance(new AlwaysGoodTestValidator());
        final ValidatorInstance secondValidator = new TestValidatorInstance(new AlwaysBadTestValidator());
        final LinkedHashSet<String> validators = new LinkedHashSet<>(Arrays.asList("first", "second"));

        expect(FieldTypeUtils.getValidator("first")).andReturn(firstValidator);
        expect(FieldTypeUtils.getValidator("second")).andReturn(secondValidator);
        replayAll();

        final int violations = ValidationUtils.validateValue(fieldValue, context, validators, "data");
        assertThat(violations, equalTo(1));
        assertThat(fieldValue.getErrorInfo().getValidation(), equalTo("second"));
        assertThat(fieldValue.getErrorInfo().getMessage(), equalTo("Always bad"));
        verifyAll();
    }

    @Test
    public void validateValueIgnoresUnknownValidators() {
        final FieldValue fieldValue = new FieldValue();
        final ValueContext context = createMock(ValueContext.class);
        final ValidatorInstance validator = new TestValidatorInstance(new AlwaysBadTestValidator());
        final LinkedHashSet<String> validators = new LinkedHashSet<>(Arrays.asList("unknown", "second"));

        expect(FieldTypeUtils.getValidator("unknown")).andReturn(null);
        expect(FieldTypeUtils.getValidator("second")).andReturn(validator);
        replayAll();

        final int violations = ValidationUtils.validateValue(fieldValue, context, validators, "data");
        assertThat(violations, equalTo(1));
        assertThat(fieldValue.getErrorInfo().getValidation(), equalTo("second"));
        assertThat(fieldValue.getErrorInfo().getMessage(), equalTo("Always bad"));
        verifyAll();
    }

    @Test
    public void validateValueIgnoresThrowingValidators() {
        final FieldValue fieldValue = new FieldValue();
        final ValueContext context = createMock(ValueContext.class);
        final ValidatorInstance throwing = createMock(ValidatorInstance.class);
        final ValidatorInstance validator = new TestValidatorInstance(new AlwaysBadTestValidator());
        final LinkedHashSet<String> validators = new LinkedHashSet<>(Arrays.asList("throwing", "second"));

        expect(FieldTypeUtils.getValidator("throwing")).andReturn(throwing);
        expect(FieldTypeUtils.getValidator("second")).andReturn(validator);
        expect(throwing.validate(isA(ValueContext.class), eq("data"))).andThrow(new RuntimeException());
        expect(context.getJcrName()).andReturn("myproject:field");
        expect(context.getJcrType()).andReturn("String");

        replayAll();

        final int violations = ValidationUtils.validateValue(fieldValue, context, validators, "data");
        assertThat(violations, equalTo(1));
        assertThat(fieldValue.getErrorInfo().getValidation(), equalTo("second"));
        assertThat(fieldValue.getErrorInfo().getMessage(), equalTo("Always bad"));
        verifyAll();
    }
}