package org.hippoecm.hst.component.support.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple value wrapper for form fields (name and data). Supports multiple value fields
 *
 * @version $Id$
 */
public class FormField {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(FormField.class);

    // field name
    private String name;
    // stored values
    private Map<String,String> values;
    // error messages
    private List<String> messages;


    public FormField(final String name) {
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


    public Map<String,String> getValues() {
        if (values == null) {
            return Collections.emptyMap();
        }
        return values;
    }

    public void setValues(final Map<String,String> values) {
        this.values = values;
    }

    public void addValue(final String value) {
        if (value == null) {
            return;
        }
        if (values == null) {
            values = new LinkedHashMap<String, String>();
        }
        values.put(value,value);
    }

    /**
     * Most of the fields have single values, we'll return first element (if there), null otherwise
     *
     * @return first value or empty string if no values present
     */
    public String getValue() {
        if (values == null || values.size() == 0) {
            return "";
        }
        return values.values().iterator().next();
    }

    public List<String> getMessages() {
        if (messages == null) {
            return Collections.emptyList();
        }
        return messages;
    }

    public void addMessage(final String value) {
        if (messages == null) {
            messages = new ArrayList<String>();
        }
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
