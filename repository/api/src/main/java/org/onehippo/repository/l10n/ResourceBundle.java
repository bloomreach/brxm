/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
 * A {@link ResourceBundle} is a set of Strings identified by keys
 * A {@link ResourceBundle} is associated with a {@link Locale}
 * making the Strings specific for that {@link Locale}.
 * {@link ResourceBundle}s can be obtained by name from the {@link LocalizationService}.
 * {@link ResourceBundle}s are ordered in a fallback hierarchy. If a String is not defined
 * in the current bundle the call is forwarded to the parent bundle.
 * See {@link LocalizationService} for an exact description.
 */
public interface ResourceBundle {

    /**
     * The {@link Locale} of this {@link ResourceBundle}.
     */
    Locale getLocale();

    /**
     * The {@code} name of this {@link ResourceBundle}.
     */
    String getName();

    /**
     * Gets the {@link Locale}-specific translation identified by a given {@code key}.
     * If a String is not defined in this bundle the call is forwarded
     * to the parent bundle.
     *
     * @param key  a {@code key} identifying a String
     * @return  the String identified by a {@code key}
     */
    String getString(String key);

}
