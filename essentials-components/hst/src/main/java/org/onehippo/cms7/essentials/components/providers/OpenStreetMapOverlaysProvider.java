/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.components.providers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;

public class OpenStreetMapOverlaysProvider implements ValueListProvider {

    private static final String BUNDLE_ID = "openstreetmap.overlays";

    @Override
    public List<String> getValues() {
        final ResourceBundle bundle = ResourceBundleUtils.getBundle(BUNDLE_ID, null);
        return Collections.list(bundle.getKeys());
    }

    @Override
    public String getDisplayValue(final String key) {
        return getDisplayValue(key, null);
    }

    @Override
    public String getDisplayValue(final String key, final Locale locale) {
        final ResourceBundle bundle = ResourceBundleUtils.getBundle(BUNDLE_ID, locale);
        final String displayValue = bundle.getString(key);
        return StringUtils.isNotBlank(displayValue) ? displayValue : key;
    }
}
