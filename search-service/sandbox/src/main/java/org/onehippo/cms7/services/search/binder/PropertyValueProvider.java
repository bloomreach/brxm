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

public interface PropertyValueProvider {

    /**
     * Method returning true when the jcr node has the property 
     * @param propertyName
     * @return true if the node has the propertyName
     */
    public boolean hasProperty(String propertyName);

    /**
     * Returns the string value of a node property
     * @param propertyName
     * @return String value of the node property, or null if the property does not exist, or is multivalued, or is not of type string
     */
    public Object getValue(String propertyName);

    /**
     * Returns the string value of a node property
     * @param propertyName
     * @return String value of the node property, or null if the property does not exist, or is multivalued, or is not of type string
     */
    public String getValueAsString(String propertyName);

    /**
     * Returns String array of string values of a node property
     * @param propertyName
     * @return String[] values of the node property, or an empty string array String[0] if the property does not exist, or is single-valued, or is not of type string
     */
    public String [] getValueAsStringArray(String propertyName);

    /**
     * Returns the boolean value of a node property
     * @param propertyName
     * @return Boolean value of the node property. If the property does not exist, or is multivalued, or is not of type boolean, false is returned
     */
    public Boolean getValueAsBoolean(String propertyName);

    /**
     * Returns boolean array of the boolean values of a node property
     * @param propertyName
     * @return Boolean[] values of the node property. If the property does not exist, or is single-valued, or is not of type boolean, an empty boolean array Boolean[0] is returned
     */
    public Boolean [] getValueAsBooleanArray(String propertyName);

    /**
     * Returns the long value of a node property.
     * @param propertyName
     * @return Long value of the node property. If the property does not exist, or is multivalued, or is not of type long, 0 is returned
     */
    public Long getValueAsLong(String propertyName);

    /**
     * Returns long array of the long values of a node property.  
     * @param propertyName
     * @return Long array presentation of the node property. If the property does not exist, or is single-valued, or is not of type long, an empty array Long[0] is returned
     */
    public Long [] getValueAsLongArray(String propertyName);

    /**
     * Returns the double value of a node property.
     * @param propertyName
     * @return Double value of the node property. If the property does not exist, or is multivalued, or is not of type double, 0 is returned
     */
    public Double getValueAsDouble(String propertyName);

    /**
     * Returns double array of the long values of a node property.  
     * @param propertyName
     * @return Double array presentation of the node property. If the property does not exist, or is single-valued, or is not of type double, an empty array Double[0] is returned
     */
    public Double [] getValueAsDoubleArray(String propertyName);

    /**
     * Returns the Calendar value of a node property.
     * @param propertyName
     * @return Calendar value of the node property. If the property does not exist, or is multivalued, or is not of type jcr DATE, null is returned
     */
    public Calendar getValueAsCalendar(String propertyName);

    /**
     * Returns Calendar array of the Calendar values of a node property.
     * @param propertyName
     * @return Calendar[] of the Calendar values of the node property. If the property does not exist, or is single-valued, or is not of type jcr DATE, an empty array Calendar[0] is returned
     */
    public Calendar [] getValueAsCalendarArray(String propertyName);

}
