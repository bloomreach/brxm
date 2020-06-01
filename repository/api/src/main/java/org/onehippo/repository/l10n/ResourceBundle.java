/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

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

    /**
     * Gets the {@link Locale}-specific translation identified by a given {@code key}. If a String is not defined in
     * this bundle the call is forwarded to the parent bundle.
     * <p/>
     * Variables in the Strings can be replaced by supplying parameters. The default definition of a variable is
     * <code>${variableName}</code>. If the label has only one variable use {@link #getString(String, String,
     * String)}.
     *
     * @param key        a {@code key} identifying a String
     * @param parameters a map of names and values to replace variables
     * @return the String identified by a {@code key} with replaced variables
     */
    default String getString(String key, Map<String, String> parameters) {
        return null;
    }

    /**
     * Gets the {@link Locale}-specific translation identified by a given {@code key}. If a String is not defined in
     * this bundle the call is forwarded to the parent bundle.
     * <p/>
     * One variable in the String can be replaced by the parameterName and parameterValue. The default definition of a
     * variable is <code>${variableName}</code>. Convenience method for Strings with only one variable. If there are
     * multiple variables use {@link #getString(String, Map)}.
     *
     * @param key            a {@code key} identifying a String
     * @param parameterName  the name of the variable as defined in the String
     * @param parameterValue the replacement value for the variable
     * @return the String identified by a {@code key} with replaced variables
     */
    default String getString(String key, String parameterName, String parameterValue) {
        return null;
    }
    
    default java.util.ResourceBundle toJavaResourceBundle() {
        return null;
    }

}
