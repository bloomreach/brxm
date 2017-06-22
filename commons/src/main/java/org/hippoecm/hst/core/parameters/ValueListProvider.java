/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.parameters;

import java.util.List;
import java.util.Locale;

/**
 * Dynamic value list provider which can be used with {@link DropDownList} annotation
 * to retrieve picker value list dynamically from any data sources.
 */
public interface ValueListProvider {

    /**
     * Return picker value list. The return value must be a non-null list object.
     *
     * @return non-null picker value list
     */
    List<String> getValues();

    /**
     * Return display string value for the picker {@code value}.
     *
     * @param value picker value
     * @return display string value for the {@code value}
     */
    String getDisplayValue(String value);

    /**
     * Return display string value for the picker {@code value} and the specified {@code locale}.
     * The {@code locale} can be null, in which case, it is supposed to use the default locale in implementations.
     *
     * @param value  picker value
     * @param locale locale
     * @return display string value for the {@code value} and the specified {@code locale}
     */
    String getDisplayValue(String value, Locale locale);

}
