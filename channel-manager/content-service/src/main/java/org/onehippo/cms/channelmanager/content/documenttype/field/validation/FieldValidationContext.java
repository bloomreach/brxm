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

package org.onehippo.cms.channelmanager.content.documenttype.field.validation;

import java.util.Locale;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms7.services.validation.field.FieldContext;

public class FieldValidationContext implements FieldContext {

    private FieldTypeContext fieldContext;
    private String type;

    public FieldValidationContext(final FieldTypeContext fieldContext, final String type) {
        this.fieldContext = fieldContext;
        this.type = type;
    }

    @Override
    public String getName() {
        return fieldContext.getName();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Session getJcrSession() {
        return fieldContext.getParentContext().getSession();
    }

    @Override
    public Locale getLocale() {
        return fieldContext.getParentContext().getLocale();
    }

}
