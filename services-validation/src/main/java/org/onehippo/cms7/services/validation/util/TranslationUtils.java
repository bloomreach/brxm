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
package org.onehippo.cms7.services.validation.util;

import java.util.Locale;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;

public class TranslationUtils {

    private static final String VALIDATORS_BUNDLE_NAME = "hippo:cms.validators";

    public static String getTranslatedMessage(final String key, final Locale locale) {
        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        if (localizationService == null) {
            return missingValue(key);
        }

        final ResourceBundle bundle = localizationService.getResourceBundle(VALIDATORS_BUNDLE_NAME, locale);
        if (bundle == null) {
            return missingValue(key);
        }

        final String value = bundle.getString(key);
        if (value == null) {
            return missingValue(key);
        }
        return value;

    }

    private static String missingValue(final String key) {
        return "???" + key + "???";
    }
}
