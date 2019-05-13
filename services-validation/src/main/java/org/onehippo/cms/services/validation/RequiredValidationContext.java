/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;

public class RequiredValidationContext implements ValidationContext {

    private final ValidationContext context;
    private final String type;

    public RequiredValidationContext(final ValidationContext context, final String type) {
        this.context = context;
        this.type = type;
    }

    @Override
    public String getJcrName() {
        return context.getJcrName();
    }

    @Override
    public String getJcrType() {
        return context.getJcrType();
    }

    @Override
    public String getType() {
        return context.getType();
    }

    @Override
    public Locale getLocale() {
        return context.getLocale();
    }

    @Override
    public TimeZone getTimeZone() {
        return context.getTimeZone();
    }

    @Override
    public Node getParentNode() {
        return context.getParentNode();
    }

    @Override
    public Node getDocumentNode() {
        return context.getDocumentNode();
    }

    @Override
    public Violation createViolation() {
        final String key = "required#" + type;
        return new TranslatedViolation(key, getLocale());
    }

    @Override
    public Violation createViolation(final String subKey) {
        final String key = "required#" + type + "#" + subKey;
        return new TranslatedViolation(key, getLocale());
    }

}
