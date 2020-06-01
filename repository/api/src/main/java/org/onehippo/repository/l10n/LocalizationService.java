/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.l10n;

import java.util.Locale;

/**
 * <p>
 * A service for obtaining {@link ResourceBundle}s from the repository.
 * The {@link ResourceBundle}s are located at /hippo:configuration/hippo:translations.
 * {@link ResourceBundle}s are identified by a combination of a name and a {@link Locale}.
 * </p>
 * <p>
 * {@link ResourceBundle} resolution is implemented as follows. A Locale is interpreted to have
 * three levels of identity, which from least to most specific are: language, country, and variant.
 * These may not all be specified, only the least specific locale identifier, the language is mandatory.
 * When a {@link ResourceBundle} is requested from the service, first an exact match is attempted, i.e.
 * a {@link ResourceBundle} is searched that matches the full identity of the given {@link Locale}.
 * If an exact match is not found a less exact match is attempted.
 * </p>
 * <p>
 * For example, if the {@link ResourceBundle}
 * for the {@link Locale} {@code th_TH_TH} is requested, first an exact match is attempted, if no such
 * {@link ResourceBundle} exists one less specific {@link ResourceBundle} is searched, i.e. {@code th_TH},
 * and if that also does not yield a result, the {@link ResourceBundle} for the {@code th} locale is looked up.
 * Finally, if still no {@link ResourceBundle} is found, a {@link ResourceBundle} with that name for the
 * {@link #DEFAULT_LOCALE} is returned if one exists.
 * </p>
 * <p>
 * In the same way, according to the same criteria of decreasing specificity of identity, {@link ResourceBundle}s
 * are linked in a fallback hierarchy: If the {@link ResourceBundle} matching the exact identity of {@code th_TH_TH}
 * does not yield a result for a specific {@link ResourceBundle#getString(String)}, the implementation falls back on
 * a match higher up in the hierarchy: first the {@link ResourceBundle} for {@code th_TH} is attempted (if it exists)
 * all the way up to the default bundle.
 * </p>
 */
public interface LocalizationService {

    /**
     * The root path in the repository where {@link ResourceBundle}s are stored.
     */
    public static final String TRANSLATIONS_PATH = "/hippo:configuration/hippo:translations";

    /**
     * The default {@link Locale} this service uses as fallback when the {@link ResourceBundle}
     * in the requested Locale is not available.
     */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * Get a {@link ResourceBundle} from the repository by name and {@link Locale}.
     * The name of a resource bundle is path relative to the {@link #TRANSLATIONS_PATH}
     * where the path elements are separated by periods (".") instead of forward slashes.
     * A {@link Locale} object identifies the {@link ResourceBundle} among the bundles identified by same path.
     * If the {@link ResourceBundle} for the requested locale is not available the implementation tries to return
     * the {@link ResourceBundle} for the default {@link Locale} identified by the same name.
     *
     * @param name  the name of the {@link ResourceBundle}
     * @param locale  the locale of the {@link ResourceBundle}
     * @return  the {@link ResourceBundle} identified by the {@code name} and {@code locale}, the {@link #DEFAULT_LOCALE}
     * if the requested locale is not available or {@code null} if no default {@link ResourceBundle} with that name exists either.
     */
    ResourceBundle getResourceBundle(final String name, final Locale locale);

}
