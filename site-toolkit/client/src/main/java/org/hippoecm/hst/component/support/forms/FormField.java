/*
 * Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.support.forms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple value wrapper for form fields (name, label, data where the data can be submitted form field values and
 * possible messages). Supports multiple value fields.
 *
 * @version $Id$
 */
public class FormField {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(FormField.class);

    // field name
    private String name;

    // label if present and otherwise null
    private String label;

    private List<String> valueList = new ArrayList<>();

    // error messages
    private List<String> messages = new ArrayList<>();

    public FormField(@JsonProperty("name") final String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("FormField name was null or empty");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the field label.
     *
     * @return label value or null.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the field label.
     *
     * @param label the label value.
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Returns the list of values of this field.
     *
     * @return values list, may be empty.
     */
    public List<String> getValueList() {
        return valueList;
    }

    /**
     * Set all values for this field. Replaces existing values. Resets values if called with null.
     *
     * @param valueList the list of values for this field. May be null to clear existing values.
     */
    public void setValueList(final List<String> valueList) {
        if (valueList == null) {
            this.valueList = new ArrayList<>();
        } else {
            this.valueList = valueList;
        }
    }

    /**
     * @deprecated use 'getValueList' instead
     */
    @Deprecated
    @JsonIgnore
    public Map<String, String> getValues() {
        Map<String, String> map = new LinkedHashMap<>(valueList.size());
        for (String s : valueList) {
            map.put(s, s);
        }
        return map;
    }

    /**
     * @deprecated use 'setValueList' instead
     */
    @Deprecated
    @JsonIgnore
    public void setValues(final Map<String, String> values) {
        if (values == null) {
            this.valueList = new ArrayList<>();
        } else {
            this.valueList = new ArrayList<>(values.values());
        }
    }

    /**
     * Adds a new value to the list of values for this field.
     *
     * @param value the new value. Null values are not added to the list.
     */
    public void addValue(final String value) {
        if (value == null) {
            return;
        }
        valueList.add(value);
    }

    /**
     * Most of the fields have a single value, we'll return first element or an empty string if there are no values available.
     *
     * @return first value or empty string if no values are present.
     */
    @JsonIgnore
    public String getValue() {
        if (valueList == null || valueList.size() == 0) {
            return "";
        }
        return valueList.get(0);
    }

    /**
     * Set messages for this field. May contain null values.
     *
     * @return list of messages, may be empty if no messages have been set.
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * Set messages for this field. Null values in the list are allowed. Resets messages if called with null.
     *
     * @param messages list of messages for this field. May be null to clear existing messages.
     */
    public void setMessages(final List<String> messages) {
        if (messages == null) {
            this.messages = new ArrayList<>();
        } else {
            this.messages = messages;
        }
    }

    /**
     * Add a message for this field. Can be called in sequence to add multiple messages.
     *
     * @param value the message. Has no effect when null.
     */
    public void addMessage(final String value) {
        messages.add(value);
    }

    /**
     * FormFields with the same name are considered equal.
     *
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FormField formField = (FormField) o;

        return !(name != null ? !name.equals(formField.name) : formField.name != null);

    }

    /**
     * Fields should be equal for same names.
     *
     * @return hash code of the field name.
     */
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

}
