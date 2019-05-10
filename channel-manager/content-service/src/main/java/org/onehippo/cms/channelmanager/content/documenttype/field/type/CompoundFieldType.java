/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationUtil;
import org.onehippo.cms.services.validation.api.FieldContext;

public class CompoundFieldType extends NodeFieldType {

    private final List<FieldType> fields = new ArrayList<>();

    public CompoundFieldType() {
        setType(Type.COMPOUND);
    }

    public List<FieldType> getFields() {
        return fields;
    }

    @Override
    public boolean isSupported() {
        return super.isSupported() && !fields.isEmpty();
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);

        return fieldContext.createContextForCompound()
                .map(context -> FieldTypeUtils.populateFields(fields, context))
                .orElse(FieldsInformation.noneSupported());
    }

    void initProviderBasedChoice(final FieldTypeContext fieldContext, final String choiceId) {
        init(fieldContext);
        setId(choiceId);
    }

    void initListBasedChoice(final ContentTypeContext parentContext, final String choiceId) {
        FieldTypeUtils.populateFields(fields, parentContext);
        setId(choiceId);
    }

    @Override
    public FieldValue readValue(final Node node) {
        Map<String, List<FieldValue>> valueMap = new HashMap<>();
        FieldTypeUtils.readFieldValues(node, getFields(), valueMap);
        return new FieldValue(valueMap);
    }

    @Override
    public boolean writeField(final FieldPath fieldPath,
                              final List<FieldValue> values,
                              final CompoundContext context) {
        return FieldTypeUtils.writeFieldNodeValue(fieldPath, values, this, context);
    }

    @Override
    public boolean writeFieldValue(final FieldPath fieldPath,
                                   final List<FieldValue> values,
                                   final CompoundContext context) {
        return FieldTypeUtils.writeFieldValue(fieldPath, values, fields, context);
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) {
        final Map<String, List<FieldValue>> valueMap = fieldValue.findFields().orElseThrow(FieldTypeUtils.INVALID_DATA);

        FieldTypeUtils.writeFieldValues(valueMap, getFields(), node);
    }

    @Override
    public int validateValue(final FieldValue value, final CompoundContext context) {
        return validateCompound(value, context)
                + FieldTypeUtils.validateFieldValues(value.getFields(), getFields(), context);
    }

    private int validateCompound(final FieldValue value, final CompoundContext context) {
        if (getValidatorNames().isEmpty()) {
            return 0;
        }

        final Object validatedValue = context.getNode();
        final FieldContext fieldContext = context.getFieldContext(getId(), getJcrType(), getEffectiveType());
        return ValidationUtil.validateValue(value, fieldContext, getValidatorNames(), validatedValue);
    }

}
