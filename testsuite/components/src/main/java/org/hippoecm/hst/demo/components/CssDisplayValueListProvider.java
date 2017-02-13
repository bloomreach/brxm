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
package org.hippoecm.hst.demo.components;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hippoecm.hst.core.parameters.ValueListProvider;

/**
 * Simple example implementation to dynamically list value picker list for a dropdown parameter.
 */
public class CssDisplayValueListProvider implements ValueListProvider {

    private static final Map<String, String> cssDisplayValuesMap = new LinkedHashMap<>();
    private static final List<String> cssDisplayValuesList;

    static {
        cssDisplayValuesMap.put("inline", "Displays an element as an inline element (like span)");
        cssDisplayValuesMap.put("block", "Displays an element as a block element (like p)");
        cssDisplayValuesMap.put("flex", "Displays an element as an block-level flex container. New in CSS3");
        cssDisplayValuesMap.put("inline-block",
                "Displays an element as an inline-level block container. The inside of this block is formatted as block-level box, and the element itself is formatted as an inline-level box");
        cssDisplayValuesMap.put("inline-flex", "Displays an element as an inline-level flex container. New in CSS3");
        cssDisplayValuesMap.put("inline-table", "The element is displayed as an inline-level tablev");
        cssDisplayValuesMap.put("list-item", "Let the element behave like a li element");
        cssDisplayValuesMap.put("run-in", "Displays an element as either block or inline, depending on context");
        cssDisplayValuesMap.put("table", "Let the element behave like a table element");
        cssDisplayValuesMap.put("table-caption", "Let the element behave like a caption element");
        cssDisplayValuesMap.put("table-column-group", "Let the element behave like a colgroup element");
        cssDisplayValuesMap.put("table-header-group", "Let the element behave like a thead element");
        cssDisplayValuesMap.put("table-footer-group", "Let the element behave like a tfoot element");
        cssDisplayValuesMap.put("table-row-group", "Let the element behave like a tbody element");
        cssDisplayValuesMap.put("table-cell", "Let the element behave like a td element");
        cssDisplayValuesMap.put("table-column", "Let the element behave like a col element");
        cssDisplayValuesMap.put("table-row", "Let the element behave like a tr element");
        cssDisplayValuesMap.put("none", "The element will not be displayed at all (has no effect on layout)");
        cssDisplayValuesMap.put("initial", "Sets this property to its default value. Read about initial");
        cssDisplayValuesMap.put("inherit", "Inherits this property from its parent element. Read about inherit");

        cssDisplayValuesList = Collections.unmodifiableList(new LinkedList<>(cssDisplayValuesMap.keySet()));
    }

    @Override
    public List<String> getValues() {
        return cssDisplayValuesList;
    }

    @Override
    public String getDisplayValue(String value) {
        return getDisplayValue(value, null);
    }

    @Override
    public String getDisplayValue(String value, Locale locale) {
        String displayValue = cssDisplayValuesMap.get(value);
        return (displayValue != null) ? displayValue : value;
    }

}
