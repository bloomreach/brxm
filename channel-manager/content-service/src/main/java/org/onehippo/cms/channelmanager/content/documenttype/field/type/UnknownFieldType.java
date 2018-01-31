/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.List;
import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;

import static org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type.UNKNOWN;

public class UnknownFieldType extends AbstractFieldType {

    public UnknownFieldType() {
        setType(UNKNOWN);
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        // Don't call super.init() to prevent doing more than needed.

        final ContentTypeItem item = fieldContext.getContentTypeItem();
        final String fieldId = item.getName();
        setId(fieldId);

        final ContentTypeContext parentContext = fieldContext.getParentContext();
        FieldTypeUtils.determineValidators(this, parentContext.getDocumentType(), item.getValidators());
    }

    @Override
    public boolean isValid() {
        // consider unknown fields always valid so their validators can be checked and used
        return true;
    }

    @Override
    protected void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, final boolean validateValues) {
        // we cannot write value of an unknown type
    }

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        return Optional.empty();
    }

    @Override
    public boolean writeField(final Node node, final FieldPath fieldPath, final List<FieldValue> values) {
        return false;
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        return false;
    }
}
