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

package org.onehippo.cms.services.validation.validator;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;

public class TestValidationContext implements ValidationContext {

    private String jcrName;
    private String jcrType;
    private String type;
    private Locale locale;
    private TimeZone timeZone;
    private Node documentNode;
    private Node parentNode;
    private boolean isViolationCreated;

    TestValidationContext() {
        this(null, null);
    }

    TestValidationContext(String jcrName, String jcrType) {
        this(jcrName, jcrType, jcrType);
    }

    TestValidationContext(String jcrName, String jcrType, String type) {
        this(jcrName, jcrType, type, null, null, null, null);
    }

    public TestValidationContext(String jcrName, String jcrType, Locale locale) {
        this(jcrName, jcrType, jcrType, locale, null, null, null);
    }

    public TestValidationContext(String jcrName, String jcrType, String type, Locale locale, TimeZone timeZone,
                                 Node parentNode, Node documentNode) {
        this.jcrName = jcrName;
        this.jcrType = jcrType;
        this.type = type;
        this.locale = locale;
        this.timeZone = timeZone;
        this.parentNode = parentNode;
        this.documentNode = documentNode;
    }

    @Override
    public String getJcrName() {
        return jcrName;
    }

    @Override
    public String getJcrType() {
        return jcrType;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }

    @Override
    public Node getParentNode() {
        return parentNode;
    }

    @Override
    public Node getDocumentNode() {
        return documentNode;
    }

    @Override
    public Violation createViolation() {
        isViolationCreated = true;
        return new TestViolation();
    }

    @Override
    public Violation createViolation(final Map<String, String> parameters) {
        return createViolation();
    }

    @Override
    public Violation createViolation(final String subKey) {
        return createViolation();
    }

    @Override
    public Violation createViolation(final String subKey, final Map<String, String> parameters) {
        return createViolation();
    }

    public boolean isViolationCreated() {
        return isViolationCreated;
    }
}
