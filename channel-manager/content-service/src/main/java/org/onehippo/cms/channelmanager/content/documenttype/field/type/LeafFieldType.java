/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.CompoundContext;

/**
 * A field that does not contain any nested fields.
 */
public abstract class LeafFieldType extends AbstractFieldType {

    @Override
    public boolean writeField(final FieldPath fieldPath, final List<FieldValue> values, final CompoundContext context) {
        if (!fieldPath.is(getId())) {
            return false;
        }
        writeValues(context.getNode(), Optional.of(values), false);
        validate(values, context);
        return true;
    }

}
