/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.BooleanUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;

public class BooleanFieldType extends PropertyFieldType {
    private static final String DEFAULT_VALUE = "false";

    public BooleanFieldType() {
        setType(Type.BOOLEAN);
    }

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }

    @Override
    public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
        return Boolean.parseBoolean(value.getValue());
    }

    @Override
    protected String fieldSpecificConversion(final String input) {
        final String output = BooleanUtils.toStringTrueFalse(BooleanUtils.toBooleanObject(input));
        if (output == null) {
            throw new IllegalArgumentException("BooleanFieldType value must be 'true' or 'false'.");
        }
        return output;
    }
}
