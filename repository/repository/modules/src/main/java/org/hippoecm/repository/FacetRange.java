/*
 *  Copyright 2008 Hippo.
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

import java.util.HashSet;
import java.util.Set;


public class FacetRange{

    private String name;
    
    // property name in format {namespace}localname
    private String namespacedProperty;
    
    private String resolution;
    
    // default begin
    private double begin = Integer.MIN_VALUE;
 
    // default end
    private double end = Integer.MAX_VALUE;
    private static final Set<String> SUPPORTED_RESOLUTIONS = new HashSet<String>();
    private static final String SUPPORTED_RESOLUTIONS_STRING = "year, month, week, day or hour";
    static {
        SUPPORTED_RESOLUTIONS.add("year");
        SUPPORTED_RESOLUTIONS.add("month");
        SUPPORTED_RESOLUTIONS.add("week");
        SUPPORTED_RESOLUTIONS.add("day");
        SUPPORTED_RESOLUTIONS.add("hour");
        //supportedResolutions.add("long");
        //supportedResolutions.add("double");
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

    public void setResolution(String resolution) {
        if(!SUPPORTED_RESOLUTIONS.contains(resolution)) {
            throw new IllegalArgumentException("Unsupported resolution '"+resolution+"'. Supported resolutions are '" + SUPPORTED_RESOLUTIONS_STRING +"'");
        }
        this.resolution = resolution;
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

}
