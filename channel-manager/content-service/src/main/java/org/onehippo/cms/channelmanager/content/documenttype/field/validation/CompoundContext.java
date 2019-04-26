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
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.FieldContextImpl;
import org.onehippo.cms.services.validation.api.FieldContext;

public class CompoundContext {

    private final Node node;
    private final Locale locale;
    private final TimeZone timeZone;

    public CompoundContext(final Node node, final Locale locale, final TimeZone timeZone) {
        this.locale = locale;
        this.timeZone = timeZone;
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public Locale getLocale() {
        return locale;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public CompoundContext getChildContext(final Node child) {
        return new CompoundContext(child, locale, timeZone);
    }

    public FieldContext getFieldContext(final String jcrName, final String jcrType, final String type) {
        return new FieldContextImpl(jcrName, jcrType, type, node, locale, timeZone);
    }
}
