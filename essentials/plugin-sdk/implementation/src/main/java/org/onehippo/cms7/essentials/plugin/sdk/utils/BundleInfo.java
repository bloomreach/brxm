/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.essentials.plugin.sdk.utils;

import org.apache.commons.lang.StringUtils;

import java.util.Locale;
import java.util.Map;

public class BundleInfo {

    private final String name;
    private final Locale locale;
    private final Map<String, String> translations;

    BundleInfo(final String name, final Locale locale, final Map<String, String> translations) {
        this.name = name;
        this.locale = locale;
        this.translations = translations;
    }

    public String getName() {
        return name;

    }

    public Locale getLocale() {
        return locale;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public String getBundlePath() {
        return StringUtils.replaceChars(name, '.', '/') + "/" + locale.toString();
    }
}
