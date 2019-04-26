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

package org.onehippo.cms.services.validation;

import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.BaseValidationContext;

public class BaseValidationContextImpl implements BaseValidationContext {

    private final String name;
    private final String type;
    private final Locale locale;
    private final TimeZone timeZone;
    private final Node parentNode;

    public BaseValidationContextImpl(final String name, final String type, final Locale locale, final TimeZone timeZone,
                                     final Node parentNode) {
        this.name = name;
        this.type = type;
        this.locale = locale;
        this.timeZone = timeZone;
        this.parentNode = parentNode;
    }

    @Override
    public String getName() {
        return name;
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
