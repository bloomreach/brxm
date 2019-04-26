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

package org.onehippo.cms.services.validation.api.internal;

import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.FieldContext;

public class FieldContextImpl implements FieldContext {

    private final String jcrName;
    private final String jcrType;
    private final String type;
    private final Locale locale;
    private final TimeZone timeZone;
    private final Node parentNode;

    public FieldContextImpl(final String jcrName, final String jcrType, final String type,
                            final Node parentNode, final Locale locale, final TimeZone timeZone) {
        this.jcrName = jcrName;
        this.jcrType = jcrType;
        this.type = type;
        this.locale = locale;
        this.timeZone = timeZone;
        this.parentNode = parentNode;
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
}
