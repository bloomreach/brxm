/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;

/**
 * LongFieldType controls the reading and writing of a Long type field from and to a node's property.
 * <p>
 * The code diligently deals with the situation that the field type definition may be out of sync with the actual
 * property value, and exposes and validates a value as consistent as possible with the field type definition. As such,
 * a "no-change" read-and-write operation may have the effect that the document is adjusted towards better consistency
 * with the field type definition.
 */
public class LongFieldType extends PropertyFieldType {

    private static final String DEFAULT_VALUE = "0";

    public LongFieldType() {
        setType(Type.LONG);
    }

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }

    @Override
    public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
        return Long.parseLong(value.getValue());
    }

    @Override
    protected String fieldSpecificConversion(final String input) {
        return Long.parseLong(input) + "";
    }

}
