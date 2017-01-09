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
 * possible messages). Supports multiple value fields
 *
 * @version $Id$
 */
public class FormField {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(FormField.class);

    // field name
    private String name;

    // label if present and otherwise {@code null
    private String label;

    private List<String> valueList = new ArrayList<>();
    // error messages
    private List<String> messages = new ArrayList<>();

    public FormField(@JsonProperty("name") final String name) {
        if(name==null || name.trim().length()==0){
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

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(final List<String> valueList) {
        if(valueList == null) {
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
    public Map<String,String> getValues() {
        Map<String, String> map = new LinkedHashMap<>(valueList.size());
        for (String s : valueList) {
            map.put(s,s);
        }
        return map;
    }

    /**
     * @deprecated use 'setValueList' instead
     */
    @Deprecated
    @JsonIgnore
    public void setValues(final Map<String,String> values) {
        if(values == null) {
            this.valueList = new ArrayList<>();
        } else {
            this.valueList = new ArrayList<>(values.values());
        }
    }

    public void addValue(final String value) {
        if (value == null) {
            return;
        }
        valueList.add(value);
    }

    /**
     * Most of the fields have single valueList, we'll return first element (if there), null otherwise
     *
     * @return first value or empty string if no valueList present
     */
    @JsonIgnore
    public String getValue() {
        if (valueList == null || valueList.size() == 0) {
            return "";
        }
        return valueList.get(0);
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(final List<String> messages) {
        if(messages == null) {
            this.messages = new ArrayList<>();
        } else {
            this.messages = messages;
        }
    }

    public void addMessage(final String value) {
        messages.add(value);
    }

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
     * Fields should be equal for same names
     *
     * @return hash code of the field name
     */
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }


}
