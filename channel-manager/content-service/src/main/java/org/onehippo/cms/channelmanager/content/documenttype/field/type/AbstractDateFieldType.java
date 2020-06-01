/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;

import javax.jcr.PropertyType;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.frontend.model.PropertyValueProvider;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;

public abstract class AbstractDateFieldType extends PrimitiveFieldType {

    private static final String DEFAULT_DISPLAY_VALUE = StringUtils.EMPTY;

    @Override
    protected int getPropertyType() {
        return PropertyType.DATE;
    }

    @Override
    protected String getDefault() {
        return DEFAULT_DISPLAY_VALUE;
    }

    @Override
    protected String fieldSpecificConversion(final String input) {
        if (StringUtils.isBlank(input)) {
            return PropertyValueProvider.EMPTY_DATE_VALUE;
        } else {
            return input;
        }
    }

    @Override
    protected FieldValue getFieldValue(final String value) {
        if (StringUtils.isBlank(value)) {
            return new FieldValue(StringUtils.EMPTY);
        }

        final Calendar calendar = ISO8601.parse(value);
        if (calendar == null || calendar.getTime().equals(PropertyValueProvider.EMPTY_DATE)) {
            return new FieldValue(StringUtils.EMPTY);
        }

        return new FieldValue(value);
    }
}
