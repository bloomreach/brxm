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
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;

public class RequiredValidationContext implements ValidationContext {

    private final ValidationContext context;
    private static final String REQUIRED_MESSAGE_PREFIX = "required";

    public RequiredValidationContext(final ValidationContext context) {
        this.context = context;
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
        return createViolation((Map<String, String>) null);
    }

    @Override
    public Violation createViolation(final Map<String, String> parameters) {
        final String typeKey = translationKey(context.getType());
        final String jcrTypeKey = translationKey(context.getJcrType());
        final TranslatedViolation violation = 
                new TranslatedViolation(getLocale(), typeKey, jcrTypeKey, REQUIRED_MESSAGE_PREFIX);
        violation.setParameters(parameters);
        return violation;
    }

    @Override
    public Violation createViolation(final String subKey) {
        return createViolation(subKey, null);
    }

    @Override
    public Violation createViolation(final String subKey, final Map<String, String> parameters) {
        final String typeKey = translationKey(context.getType()) + "#" + subKey;
        final String jcrTypeKey = translationKey(context.getJcrType()) + "#" + subKey;
        final TranslatedViolation violation = 
                new TranslatedViolation(getLocale(), typeKey, jcrTypeKey, REQUIRED_MESSAGE_PREFIX);
        violation.setParameters(parameters);
        return violation;
    }

    private String translationKey(final String typeName) {
        // A resource bundle key must be a valid JCR property name. In "{@code resource#hippo:mirror}" the part before
        // the colon would be used as a namespace prefix. Prefixes cannot contain characters like # or _. Replacing the
        // colon with a dash avoids this problem.
        final String cleanTypeName = StringUtils.replace(typeName, ":", "-");

        return REQUIRED_MESSAGE_PREFIX + "#" + cleanTypeName;
    }
}
