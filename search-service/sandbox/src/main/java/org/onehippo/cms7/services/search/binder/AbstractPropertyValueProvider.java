/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.binder;

import java.util.Calendar;

import org.onehippo.cms7.services.search.binder.PropertyValueProvider;

public abstract class AbstractPropertyValueProvider implements PropertyValueProvider {

    @Override
    public abstract boolean hasProperty(String propertyName);

    @Override
    public abstract Object getValue(String propertyName);

    @Override
    public String getValueAsString(String propertyName) {
        return null;
    }

    @Override
    public String [] getValueAsStringArray(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        if (!valueObj.getClass().isArray()) {
            throw new IllegalArgumentException("The property value is not an array.");
        }

        return (String []) valueObj;
    }

    @Override
    public Boolean getValueAsBoolean(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        return (Boolean) valueObj;
    }

    @Override
    public Boolean [] getValueAsBooleanArray(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        if (!valueObj.getClass().isArray()) {
            throw new IllegalArgumentException("The property value is not an array.");
        }

        return (Boolean []) valueObj;
    }

    @Override
    public Long getValueAsLong(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        return (Long) valueObj;
    }

    @Override
    public Long [] getValueAsLongArray(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        if (!valueObj.getClass().isArray()) {
            throw new IllegalArgumentException("The property value is not an array.");
        }

        return (Long []) valueObj;
    }

    @Override
    public Double getValueAsDouble(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        return (Double) valueObj;
    }

    @Override
    public Double [] getValueAsDoubleArray(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        if (!valueObj.getClass().isArray()) {
            throw new IllegalArgumentException("The property value is not an array.");
        }

        return (Double []) valueObj;
    }

    @Override
    public Calendar getValueAsCalendar(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        return (Calendar) valueObj;
    }

    @Override
    public Calendar [] getValueAsCalendarArray(String propertyName) {
        if (!hasProperty(propertyName)) {
            return null;
        }

        Object valueObj = getValue(propertyName);

        if (valueObj == null) {
            return null;
        }

        if (!valueObj.getClass().isArray()) {
            throw new IllegalArgumentException("The property value is not an array.");
        }

        return (Calendar []) valueObj;
    }

}
