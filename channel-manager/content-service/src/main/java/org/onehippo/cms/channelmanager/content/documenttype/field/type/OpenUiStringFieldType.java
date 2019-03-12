/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

/**
 * OpenUIStringFieldType controls the reading and writing of an OpenUIString type field from and to a node's property.
 * <p>
 * The code diligently deals with the situation that the field type definition may be out of sync with the actual
 * property value, and exposes and validates a value as consistent as possible with the field type definition. As such,
 * a "no-change" read-and-write operation may have the effect that the document is adjusted towards better consistency
 * with the field type definition.
 */
public class OpenUiStringFieldType extends PrimitiveFieldType {

    private static final String DEFAULT_VALUE = StringUtils.EMPTY;

    private String uiExtension = null;
    
    public OpenUiStringFieldType() {
        setType(Type.OPEN_UI);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        fieldContext.getStringConfig("uiExtension").ifPresent(this::setUiExtension);
        return super.init(fieldContext);
    }

    private void setUiExtension(final String uiExtension) {
        this.uiExtension = uiExtension;
    }

    String getUiExtension() {
        return uiExtension;
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.STRING;
    }

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }
}
