/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.repository.api.Localized;

/**
 * Creates the message shown to a user when a document or folder is renamed.
 */
public class RenameMessage {

    private final Locale locale;
    private final Map<Localized,String> localizedNames;

    public RenameMessage(final Locale locale, final Map<Localized, String> localizedNames) {
        this.locale = locale;
        this.localizedNames = localizedNames;
    }

    public boolean shouldShow() {
        final Localized bestMatch = bestMatchingLocalizedOrNull();
        if (localizedNames.containsKey(bestMatch)) {
            return localizedNames.size() > 1;
        }
        return !localizedNames.isEmpty();
    }

    private Localized bestMatchingLocalizedOrNull() {
        Localized bestMatch = Localized.getInstance(locale);
        if (localizedNames.containsKey(bestMatch)) {
            return bestMatch;
        }
        bestMatch = Localized.getInstance();
        if (localizedNames.containsKey(bestMatch)) {
            return bestMatch;
        }
        return null;
    }

    public String forDocument() {
        return createMessage("message-document");
    }

    public String forFolder() {
        return createMessage("message-folder");
    }

    private String createMessage(final String key) {
        final String forLocales = forLocalesMessage(localizedNames);
        return getLabel(key, forLocales);
    };

    private String forLocalesMessage(final Map<Localized, String> localizedNames) {
        List<String> forLocales = new ArrayList<String>(localizedNames.size());

        for (Map.Entry<Localized, String> entry : localizedNames.entrySet()) {
            final Locale locale = entry.getKey().getLocale();
            final String language = locale == null ? getLabel("default-locale") : locale.getLanguage();
            final String name = entry.getValue();
            final String forLocale = getLabel("for-locale", language, name);
            forLocales.add(forLocale);
        }

        return StringUtils.join(forLocales, ", ");
    }

    private String getLabel(final String key, final Object... parameters) {
        final ClassResourceModel resourceModel = new ClassResourceModel(key, RenameMessage.class, parameters);
        return resourceModel.getObject();
    }

}
