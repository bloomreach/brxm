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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.List;
import java.util.Optional;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FieldTypeTest {

    private FieldType fieldType;

    @Before
    public void setup() {
        fieldType = new FieldType() {
            @Override
            public Optional<List<FieldValue>> readFrom(final Node node) {
                return null;
            }

            @Override
            public void writeTo(final Node node, final Optional<Object> optionalValue) throws ErrorWithPayloadException {

            }

            @Override
            public boolean validate(final List<FieldValue> valueList) {
                return false;
            }
        };
    }

    @Test
    public void hasUnsupportedValidator() {
        assertFalse(fieldType.hasUnsupportedValidator());
        fieldType.addValidator(FieldType.Validator.UNSUPPORTED);
        assertTrue(fieldType.hasUnsupportedValidator());
        fieldType.getValidators().remove(FieldType.Validator.UNSUPPORTED);
        assertFalse(fieldType.hasUnsupportedValidator());
    }
}
