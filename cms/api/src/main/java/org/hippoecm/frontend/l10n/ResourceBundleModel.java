/*
 *  Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;

public class ResourceBundleModel extends LoadableDetachableModel<String> {

    private final String bundleName;
    private final String key;
    private final Locale locale;
    private final Map<String, String> parameters;
    private final String missingValue;

    public static class Builder {
        // required parameters
        private final String bundleName;
        private final String key;

        // optional parameters, set to default values
        private Locale locale = null;
        private String defaultValue = null;
        // keeps track of whether a defaultValue is set. This is needed because defaultValue parameter {@code null}
        // results in slightly different behavior than the constructor without defaultValue
        private boolean defaultValueSet = false;
        private Map<String, String> parameters = null;

        public Builder(final String bundleName, final String key) {
            this.bundleName = bundleName;
            this.key = key;
        }

        /**
         * To set a Locale. When not set by default {@link Session#getLocale()} is used.
         * @param locale the bundle locale
         */
        public Builder locale(final Locale locale) {
            this.locale = locale;
            return this;
        }

        public Builder defaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
            this.defaultValueSet = true;
            return this;
        }

        public Builder parameters(final Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public ResourceBundleModel build() {
            return new ResourceBundleModel(this);
        }
    }

    public static ResourceBundleModel of(final String bundleName, final String key) {
        return new Builder(bundleName, key).build();
    }

    private ResourceBundleModel(final Builder builder) {
        this.bundleName = builder.bundleName;
        this.key = builder.key;
        this.locale = builder.locale;
        this.parameters = builder.parameters;

        missingValue = builder.defaultValueSet
                ? builder.defaultValue
                : "???" + key + "???";
    }

    @Override
    protected String load() {
        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        if (localizationService == null) {
            return missingValue();
        }

        final Locale locale = this.locale != null ? this.locale : Session.get().getLocale();
        final ResourceBundle bundle = localizationService.getResourceBundle(bundleName, locale);
        if (bundle == null) {
            return missingValue();
        }

        final String value = parameters == null
                ? bundle.getString(key)
                : bundle.getString(key, parameters);

        return value == null
                ? missingValue()
                : value;
    }

    private String missingValue() {
        return missingValue;
    }

    @Override
    public void setObject(final String object) {
    }
}
