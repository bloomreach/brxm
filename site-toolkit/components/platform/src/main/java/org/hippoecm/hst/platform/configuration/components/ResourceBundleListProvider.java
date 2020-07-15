/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.configuration.components;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides values and their labels that are defined as resource bundle documents.
 * it doesn't look up Java standard resource bundles in case the bundle is not found.
 */
public class ResourceBundleListProvider implements ValueListProvider {

    private static final Logger log = LoggerFactory.getLogger(ResourceBundleListProvider.class);

    private final String resourceBundleId;

    public ResourceBundleListProvider(final String resourceBundleId) {
        this.resourceBundleId = resourceBundleId;
    }

    @Override
    public List<String> getValues() {
        try {
            final ResourceBundle bundle = ResourceBundleUtils.getBundle(resourceBundleId, null, false);
            return Collections.list(bundle.getKeys());
        } catch (MissingResourceException e) {
            log.warn("The resource bundle document is not found, bundle id: {}", resourceBundleId);
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public String getDisplayValue(String value) {
        return getDisplayValue(value, null);
    }

    @Override
    public String getDisplayValue(String value, Locale locale) {
        final ResourceBundle bundle = ResourceBundleUtils.getBundle(resourceBundleId, locale, false);
        final String displayValue = bundle.getString(value);
        return StringUtils.isNotBlank(displayValue) ? displayValue : resourceBundleId;
    }

}
