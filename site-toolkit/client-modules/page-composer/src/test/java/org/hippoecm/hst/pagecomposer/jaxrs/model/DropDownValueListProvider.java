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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hippoecm.hst.core.parameters.ValueListProvider;

public class DropDownValueListProvider implements ValueListProvider {

    private static final Locale defaultLocale = Locale.ENGLISH;
    private static final Map<Locale, HashMap<String, String>> valuesByLocale = new HashMap<>();
    
    static {
        HashMap<String, String> enValues = new HashMap<>();
        enValues.put("key1", "Value One");
        enValues.put("key2", "Value Two");
        valuesByLocale.put(defaultLocale, enValues);
        HashMap<String, String> frValues = new HashMap<>();
        frValues.put("key1", "Valeur un");
        frValues.put("key2", "Valeur deux");
        valuesByLocale.put(Locale.FRENCH, frValues);
    }
    
    @Override
    public List<String> getValues() {
        return Collections.unmodifiableList(new LinkedList<>(valuesByLocale.get(defaultLocale).keySet()));
    }

    @Override
    public String getDisplayValue(final String value) {
        return valuesByLocale.get(defaultLocale).get(value);
    }

    @Override
    public String getDisplayValue(final String value, final Locale locale) {
        return locale == null ? valuesByLocale.get(defaultLocale).get(value) : valuesByLocale.get(locale).get(value);
    }
}