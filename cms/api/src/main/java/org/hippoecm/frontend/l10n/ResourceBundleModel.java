/*
 *  Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.l10n;

import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;

public class ResourceBundleModel extends LoadableDetachableModel<String> {

    private final String bundleName;
    private final String key;
    private final Locale locale;
    private final String defaultValue;
    // keeps track of whether a defaultValue is set. This is needed because defaultValue parameter {@code null} results
    // in slightly different behavior than the constructor without defaultValue
    private final boolean defaultValueSet;

    public ResourceBundleModel(final String bundleName, final String key) {
        this(bundleName, key, null, false, null);
    }
    public ResourceBundleModel(final String bundleName, final String key, final String defaultValue) {
        this(bundleName, key, defaultValue, true, null);
    }

    public ResourceBundleModel(final String bundleName, final String key, final Locale locale) {
        this(bundleName, key, null, false, locale);
    }

    public ResourceBundleModel(final String bundleName, final String key, final String defaultValue, final Locale locale) {
        this(bundleName, key, defaultValue, true, locale);
    }

    private ResourceBundleModel(final String bundleName, final String key, final String defaultValue, final boolean defaultValueSet, final Locale locale) {
        this.bundleName = bundleName;
        this.key = key;
        this.defaultValue = defaultValue;
        this.defaultValueSet = defaultValueSet;
        this.locale = locale;
    }

    @Override
    protected String load() {
        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        if (localizationService != null) {
            Locale locale = this.locale != null ? this.locale : Session.get().getLocale();
            final ResourceBundle bundle = localizationService.getResourceBundle(bundleName, locale);
            if (bundle != null) {
                final String value = bundle.getString(key);
                if (value != null) {
                    return value;
                }
                return missingValue();
            }
        }
        return missingValue();
    }

    private String missingValue() {
        if (defaultValueSet) {
            return defaultValue;
        }
        return "???" + key + "???";
    }

    @Override
    public void setObject(final String object) {
    }
}
