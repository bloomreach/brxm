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

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

public class BooleanFieldType extends PrimitiveFieldType {
    private static final String DEFAULT_VALUE = "false";

    public BooleanFieldType() {
        setType(Type.BOOLEAN);
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.BOOLEAN;
    }

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }

    @Override
    protected String fieldSpecificConversion(final String input) {
        return Boolean.parseBoolean(input) + "";
    }
}
