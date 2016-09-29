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

package org.onehippo.cms.channelmanager.content.util;

import java.util.Arrays;

import javax.jcr.Node;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.model.DocumentTypeSpec;
import org.onehippo.cms.channelmanager.content.model.FieldTypeSpec;
import org.onehippo.cms.channelmanager.content.util.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NamespaceUtils.class)
public class FieldTypeUtilsTest {
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";

    @Test
    public void validateSupportedFieldTypes() {
        final ContentTypeProperty property = createMock(ContentTypeProperty.class);

        expect(property.getItemType()).andReturn("String");
        replay(property);
        assertThat(FieldTypeUtils.isSupportedFieldType(property), equalTo(true));
        reset(property);

        expect(property.getItemType()).andReturn("Text");
        replay(property);
        assertThat(FieldTypeUtils.isSupportedFieldType(property), equalTo(true));
        reset(property);

        expect(property.getItemType()).andReturn("Html");
        replay(property);
        assertThat(FieldTypeUtils.isSupportedFieldType(property), equalTo(false));
        reset(property);

        expect(property.getItemType()).andReturn(null);
        replay(property);
        assertThat(FieldTypeUtils.isSupportedFieldType(property), equalTo(false));
    }

    @Test
    public void validateProjectPropertyDetection() {
        final ContentTypeProperty property = createMock(ContentTypeProperty.class);

        expect(property.getName()).andReturn("anything:unspecific");
        replay(property);
        assertThat(FieldTypeUtils.isProjectProperty(property), equalTo(true));
        reset(property);

        expect(property.getName()).andReturn("unnamespaced");
        replay(property);
        assertThat(FieldTypeUtils.isProjectProperty(property), equalTo(true));
        reset(property);

        expect(property.getName()).andReturn("hippo:something");
        replay(property);
        assertThat(FieldTypeUtils.isProjectProperty(property), equalTo(false));
        reset(property);
    }

    @Test
    public void validatePluginClassValidation() {
        final ContentTypeProperty property = createMock(ContentTypeProperty.class);
        final Node documentTypeRootNode = createMock(Node.class);
        PowerMock.mockStatic(NamespaceUtils.class);

        expect(property.getName()).andReturn("dummy").anyTimes();
        expect(property.getItemType()).andReturn("String").anyTimes();
        replay(property);

        // deals with null value
        expect(NamespaceUtils.getPluginClassForField(documentTypeRootNode, "dummy")).andReturn(null);
        PowerMock.replayAll();
        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(property, documentTypeRootNode), equalTo(false));
        PowerMock.verifyAll();

        // rejects dummy text
        PowerMock.resetAll();
        expect(NamespaceUtils.getPluginClassForField(documentTypeRootNode, "dummy")).andReturn("sometext");
        PowerMock.replayAll();
        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(property, documentTypeRootNode), equalTo(false));

        // accept correct value for both String and Text
        reset(property);
        expect(property.getName()).andReturn("dummy").anyTimes();
        expect(property.getItemType()).andReturn("String");
        expect(property.getItemType()).andReturn("Text");
        replay(property);

        PowerMock.resetAll();
        expect(NamespaceUtils.getPluginClassForField(documentTypeRootNode, "dummy")).andReturn(PROPERTY_FIELD_PLUGIN).anyTimes();
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(property, documentTypeRootNode), equalTo(true));
        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(property, documentTypeRootNode), equalTo(true));
    }

    @Test
    public void validateDeriveFieldType() {
        final ContentTypeProperty property = createMock(ContentTypeProperty.class);

        expect(property.getItemType()).andReturn("String");
        expect(property.getItemType()).andReturn("Text");
        replay(property);

        assertThat(FieldTypeUtils.deriveFieldType(property), equalTo(FieldTypeSpec.Type.STRING));
        assertThat(FieldTypeUtils.deriveFieldType(property), equalTo(FieldTypeSpec.Type.MULTILINE_STRING));
    }

    @Test(expected = IllegalStateException.class)
    public void validateDeriveFieldTypeOfUnknownType() {
        final ContentTypeProperty property = createMock(ContentTypeProperty.class);

        expect(property.getItemType()).andReturn("Html");
        replay(property);

        FieldTypeUtils.deriveFieldType(property);
    }

    @Test
    public void validateIgnoredValidator() {
        final FieldTypeSpec fieldType = createMock(FieldTypeSpec.class);
        replay(fieldType);

        FieldTypeUtils.determineValidators(fieldType, Arrays.asList("optional"));
    }

    @Test
    public void validateMappedValidators() {
        final FieldTypeSpec fieldType = createMock(FieldTypeSpec.class);

        fieldType.addValidator(FieldTypeSpec.Validator.REQUIRED);
        expectLastCall();
        fieldType.addValidator(FieldTypeSpec.Validator.REQUIRED);
        expectLastCall();
        replay(fieldType);

        FieldTypeUtils.determineValidators(fieldType, Arrays.asList("required", "non-empty"));
    }

    @Test
    public void validateFieldValidators() {
        final FieldTypeSpec fieldType = createMock(FieldTypeSpec.class);

        fieldType.addValidator(FieldTypeSpec.Validator.UNSUPPORTED);
        expectLastCall();
        fieldType.addValidator(FieldTypeSpec.Validator.UNSUPPORTED);
        expectLastCall();
        replay(fieldType);

        FieldTypeUtils.determineValidators(fieldType, Arrays.asList("email", "references"));
    }

    @Test
    public void validateUnknownValidators() {
        final FieldTypeSpec fieldType = createMock(FieldTypeSpec.class);
        final DocumentTypeSpec docType = createMock(DocumentTypeSpec.class);

        expect(fieldType.getDocumentTypeSpec()).andReturn(docType);
        docType.setReadOnlyDueToUnknownValidator(true);
        expectLastCall();
        replay(fieldType, docType);

        FieldTypeUtils.determineValidators(fieldType, Arrays.asList("unknown-validator"));
    }
}
