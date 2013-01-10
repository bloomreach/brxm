/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.PropertyType;

public class FacetRange {

    private String name;

    // property name in format {namespace}localname
    private String namespacedProperty;

    private String resolution;

    private int rangeType;

    // default begin (for date/double and long ranges)
    private double begin = Double.NEGATIVE_INFINITY;

    // default end (for date/double and long ranges)
    private double end = Double.POSITIVE_INFINITY;

    // for String based ranges
    private String lower;

    // for String based ranges
    private String upper;

    private static final Map<String, Integer> SUPPORTED_RESOLUTIONS = new HashMap<String, Integer>();

    static {
        SUPPORTED_RESOLUTIONS.put("year", PropertyType.DATE);
        SUPPORTED_RESOLUTIONS.put("month", PropertyType.DATE);
        SUPPORTED_RESOLUTIONS.put("week", PropertyType.DATE);
        SUPPORTED_RESOLUTIONS.put("day", PropertyType.DATE);
        SUPPORTED_RESOLUTIONS.put("hour", PropertyType.DATE);
        SUPPORTED_RESOLUTIONS.put("long", PropertyType.LONG);
        SUPPORTED_RESOLUTIONS.put("double", PropertyType.DOUBLE);
        SUPPORTED_RESOLUTIONS.put("string", PropertyType.STRING);
    }

    public String getNamespacedProperty() {
        return namespacedProperty;
    }

    public void setNamespacedProperty(String namespacedProperty) {
        this.namespacedProperty = namespacedProperty;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResolution() {
        return resolution;
    }

    /*
     * returns the type of range as an integer according javax.jcr.PropertyType values.
     */
    public int getRangeType() {
        return this.rangeType;
    }

    public void setResolution(String resolution) {
        if (!SUPPORTED_RESOLUTIONS.containsKey(resolution)) {
            StringBuilder supportedResolutions = new StringBuilder();
            for (String res : SUPPORTED_RESOLUTIONS.keySet()) {
                if (supportedResolutions.length() != 0) {
                    supportedResolutions.append(", ");
                }
                supportedResolutions.append(res);
            }
            supportedResolutions.insert(0, "[").append("]");
            throw new IllegalArgumentException("Unsupported resolution '" + resolution
                    + "'. Supported resolutions are '" + supportedResolutions.toString() + "'");
        }
        this.resolution = resolution;
        this.rangeType = SUPPORTED_RESOLUTIONS.get(resolution);
    }

    public double getBegin() {
        return begin;
    }

    public void setBegin(double begin) {
        this.begin = begin;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }

    public String getLower() {
        return lower;
    }

    public void setLower(String lower) {
        if (this.upper != null && lower != null) {
            if (this.upper.length() != lower.length() || lower.length() == 0) {
                throw new IllegalArgumentException(
                        "Unsupported String range: the lower and upper bound need to exist of equal number of chars");
            } else if (lower.length() > 3) {
                throw new IllegalArgumentException(
                        "Unsupported String range: number of chars in range for lower is not allowed to exceed 3 or to be 0");
            }
        }
        this.lower = lower;
    }

    public String getUpper() {
        return upper;
    }

    public void setUpper(String upper) {
        if (this.lower != null && upper != null) {
            if (this.lower.length() != upper.length()) {
                throw new IllegalArgumentException(
                        "Unsupported String range: the lower and upper bound need to exist of equal number of chars");
            } else if (upper.length() > 3 || upper.length() == 0) {
                throw new IllegalArgumentException(
                        "Unsupported String range: number of chars in range for upper is not allowed to exceed 3 or to be 0");
            }
        }
        this.upper = upper;
    }

}
