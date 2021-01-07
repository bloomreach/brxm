/*
 *  Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.repository.l10n.ResourceBundle;
import org.powermock.api.easymock.PowerMock;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

public class MockFieldTypeContext {

    public static final String DEFAULT_JCR_NAME = "field:id";
    public static final String DEFAULT_JCR_TYPE = "String";

    private MockFieldTypeContext() {}

    public static class Builder {

        private final AbstractFieldType fieldType;

        private String jcrName = DEFAULT_JCR_NAME;
        private String jcrType = DEFAULT_JCR_TYPE;
        private String type = "Text";
        private boolean isMultiple = false;
        private String hint;
        private String displayName;
        private List<String> validators = Collections.emptyList();
        private ResourceBundle resourceBundle;
        private Node editorFieldNode;
        private Integer maxValues;
        private Locale parentContextLocale;

        public Builder(final AbstractFieldType fieldType) {
            this.fieldType = fieldType;
        }

        public Builder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder jcrName(final String jcrName) {
            this.jcrName = jcrName;
            return this;
        }

        public Builder jcrType(final String jcrType) {
            this.jcrType = jcrType;
            return this;
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public Builder validators(final List<String> validators) {
            this.validators = validators;
            return this;
        }

        public Builder hint(final String hint) {
            this.hint = hint;
            return this;
        }

        public Builder multiple(final boolean isMultiple) {
            this.isMultiple = isMultiple;
            return this;
        }

        public Builder resourceBundle(final ResourceBundle resourceBundle) {
            this.resourceBundle = resourceBundle;
            return this;
        }

        public Builder editorFieldNode(final Node editorFieldNode) {
            this.editorFieldNode = editorFieldNode;
            return this;
        }

        public Builder parentContextLocale(final Locale locale) {
            this.parentContextLocale = locale;
            return this;
        }

        public Builder maxValues(final Integer maxValues) {
            this.maxValues = maxValues;
            return this;
        }

        public FieldTypeContext build() {
            final FieldTypeContext fieldContext = PowerMock.createMock(FieldTypeContext.class);

            PowerMock.mockStaticPartial(FieldTypeUtils.class, "determineValidators");
            FieldTypeUtils.determineValidators(fieldType, fieldContext, validators);
            expectLastCall();

            final Optional<ResourceBundle> optionalResourceBundle = Optional.ofNullable(resourceBundle);
            final Optional<Node> optionalEditorFieldNode = Optional.ofNullable(editorFieldNode);
            PowerMock.mockStaticPartial(LocalizationUtils.class, "determineFieldDisplayName", "determineFieldHint");
            expect(LocalizationUtils.determineFieldDisplayName(jcrName, optionalResourceBundle, optionalEditorFieldNode))
                    .andReturn(Optional.ofNullable(displayName));

            expect(LocalizationUtils.determineFieldHint(jcrName, optionalResourceBundle, optionalEditorFieldNode))
                    .andReturn(Optional.ofNullable(hint));

            final ContentTypeContext parentContext = PowerMock.createMock(ContentTypeContext.class);
            if (parentContextLocale != null) {
                expect(parentContext.getLocale()).andReturn(parentContextLocale);
            }
            expect(parentContext.getResourceBundle()).andReturn(optionalResourceBundle);

            expect(fieldContext.getParentContext()).andReturn(parentContext).anyTimes();
            expect(fieldContext.getEditorConfigNode()).andReturn(optionalEditorFieldNode).anyTimes();
            expect(fieldContext.getValidators()).andReturn(validators);
            expect(fieldContext.getJcrName()).andReturn(jcrName);
            expect(fieldContext.getJcrType()).andReturn(jcrType);
            expect(fieldContext.getType()).andReturn(type);
            expect(fieldContext.isMultiple()).andReturn(isMultiple).anyTimes();

            if (isMultiple) {
                final Optional<String> maxItemsOptional = maxValues != null
                        ? Optional.of(String.valueOf(maxValues))
                        : Optional.empty();
                expect(fieldContext.getStringConfig("maxitems")).andReturn(maxItemsOptional);
            }

            return fieldContext;
        }
    }
}
