/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.resource.loader.IStringResourceLoader;

/**
 * Resource model that uses the wicket string resource locator mechanism, but not limited
 * to {@link Component}s.
 */
public class ClassResourceModel extends LoadableDetachableModel<String> {

    private final Class<?> clazz;
    private final String key;
    private final Locale locale;
    private final String style;
    private final Object[] parameters;

    public ClassResourceModel(final String key, final Class<?> clazz, final Object... parameters) {
        this(key, clazz, Session.get().getLocale(), Session.get().getStyle(), parameters);
    }

    public ClassResourceModel(final String key, final Class<?> clazz, final Locale locale, final String style, final Object... parameters) {
        this.clazz = clazz;
        this.key = key;
        this.locale = locale;
        this.style = style;
        this.parameters = parameters;
    }

    @Override
    protected String load() {
        final Iterator<IStringResourceLoader> iter = Application.get().getResourceSettings().getStringResourceLoaders()
                .iterator();
        String value = null;
        while (iter.hasNext()) {
            final IStringResourceLoader loader = iter.next();
            value = loader.loadStringResource(clazz, key, locale, style, null);
            if (value != null) {
                break;
            }
        }
        if (value != null) {
            if (parameters != null) {
                final MessageFormat format = new MessageFormat(value, locale);
                value = format.format(parameters);
            }
            return value;
        }

        if (RuntimeConfigurationType.DEVELOPMENT.equals(Application.get().getConfigurationType())) {
            throw new RuntimeException("No translation found for " + this);
        } else {
            return key;
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("class", clazz.getName())
            .append("key", key)
            .append("locale", locale)
            .append("style", style)
            .toString();
    }
}
