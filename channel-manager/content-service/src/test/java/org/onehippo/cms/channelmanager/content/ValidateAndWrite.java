/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.cms.channelmanager.content;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;

public class ValidateAndWrite {

    public static void validateAndWriteTo(final Node document, final FieldType fieldType, final List<FieldValue> fieldValues) {
        validateAndWriteTo(document, document, fieldType, fieldValues);
    }

    public static void validateAndWriteTo(final Node node, final Node document,  final FieldType fieldType, final List<FieldValue> fieldValues) {
        int validate = fieldType.validate(fieldValues, new CompoundContext(node ,document, Locale.ENGLISH, TimeZone.getTimeZone(ZonedDateTime.now().getZone())));
        if (validate == 0){
            fieldType.writeTo(node, Optional.ofNullable(fieldValues));
        }
    }
}
