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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

public class PropertyFieldType extends FieldType {
    @JsonIgnore
    private boolean storedAsMultiValueProperty;

    @Override
    public Optional<FieldType> init(final FieldTypeContext context,
                                    final ContentTypeContext contentTypeContext,
                                    final DocumentType docType) {
        return super.init(context, contentTypeContext, docType)
                .map(fieldType -> {
                    setStoredAsMultiValueProperty(context.getContentTypeItem().isMultiple());
                    return this;
                });
    }

    public boolean isStoredAsMultiValueProperty() {
        return storedAsMultiValueProperty;
    }

    public void setStoredAsMultiValueProperty(final boolean storedAsMultiValueProperty) {
        this.storedAsMultiValueProperty = storedAsMultiValueProperty;
    }
}
