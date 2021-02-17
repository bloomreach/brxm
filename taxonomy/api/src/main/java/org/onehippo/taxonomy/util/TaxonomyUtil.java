/*
 *  Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyUtil {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyUtil.class);
    static final String PROTOTYPE_LOCALE = "document-type-locale";

    /**
     * Builds a list of Locales from Strings.
     * @param localeStrings {@link #toLocale}
     * @return a list of Locales
     */
    public static List<Locale> getLocalesList(final String[] localeStrings) {
        if (localeStrings == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(localeStrings)
                .map(TaxonomyUtil::toLocale)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static boolean isPrototypeLocale(final String localeString) {
        return PROTOTYPE_LOCALE.equals(localeString);
    }

    /**
     * Creates a Locale from a String.
     * @param localeString can be in Java Locale#toString format or a LanguageTag as described by the IETF BCP 47
     *                     specification. For example "en_GB" and "en-GB" will result in the same Locale object.
     * @return null if localeString was null or not representing a valid Locale, or the requested Locale
     */
    public static Locale toLocale(final String localeString) {
        if (localeString == null || isPrototypeLocale(localeString)) {
            return null;
        }
        try {
            if (localeString.contains("_")) {
                return LocaleUtils.toLocale(localeString);
            } else {
                final Locale locale = Locale.forLanguageTag(localeString);
                // fallback for invalid language tags: try the more lenient LocaleUtils.
                if (!locale.toLanguageTag().equals(localeString)) {
                    return LocaleUtils.toLocale(localeString);
                }
                return locale;
            }
        } catch (IllegalArgumentException e) {
            log.error("Locale \"{}\" is not valid.", localeString);
            return null;
        }
    }

    private TaxonomyUtil () {}

}
