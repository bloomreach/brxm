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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;

class TranslatedViolation implements Violation {

    private static final String VALIDATORS_BUNDLE_NAME = "hippo:cms.validators";

    private final Locale locale;
    private final List<String> keys = new ArrayList<>();
    private Map<String, String> parameters = null;

    TranslatedViolation(final Locale locale, final String key, final String... fallbackKeys) {
        this.locale = locale;
        keys.add(key);
        keys.addAll(Arrays.asList(fallbackKeys));
    }

    String getFirstKey() {
        return keys.get(0);
    }

    List<String> getKeys() {
        return keys;
    }

    Locale getLocale() {
        return locale;
    }

    void setParameters(final Map<String, String> parameters) {
        this.parameters = parameters;    
    }
    
    @Override
    public String getMessage() {
        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        if (localizationService == null) {
            return missingValue();
        }

        final ResourceBundle bundle = localizationService.getResourceBundle(VALIDATORS_BUNDLE_NAME, locale);
        if (bundle == null) {
            return missingValue();
        }

        return keys.stream()
                .map(key -> bundle.getString(key, parameters))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(missingValue());
    }

    private String missingValue() {
        return "???" + getFirstKey() + "???";
    }
}
