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
package org.hippoecm.hst.provider.jcr;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.hippoecm.hst.provider.PropertyMap;

public class PropertyMapImpl implements PropertyMap, Serializable {

    private static final long serialVersionUID = 1L;
    
    private Map<String, Boolean> booleans = new HashMap<String, Boolean>();
    private Map<String, Boolean[]> booleanArrays = new HashMap<String, Boolean[]>();

    private Map<String, String> strings = new HashMap<String, String>();
    private Map<String, String[]> stringArrays = new HashMap<String, String[]>();

    private Map<String, Double> doubles = new HashMap<String, Double>();
    private Map<String, Double[]> doubleArrays = new HashMap<String, Double[]>();

    private Map<String, Long> longs = new HashMap<String, Long>();
    private Map<String, Long[]> longArrays = new HashMap<String, Long[]>();

    private Map<String, Calendar> calendars = new HashMap<String, Calendar>();
    private Map<String, Calendar[]> calendarArrays = new HashMap<String, Calendar[]>();

    private Set<String> availableProps = new HashSet<String>();
    private Set<String> unAvailableProps = new HashSet<String>();
    
    public boolean hasProperty(String name) {
        if(availableProps == null) {
            return false;
        }
        return availableProps.contains(name);
    }
    
    public void addAvailableProperty(String name) {
        if(availableProps == null) {
            throw new IllegalStateException("availableProps is already nullified. Cannot call addAvailableProperty at this point");
        }
        availableProps.add(name);
    }
    
    public boolean isUnAvailableProperty(String name) {
        if(unAvailableProps == null) {
            return false;
        }
        return unAvailableProps.contains(name);
    }
    
    public void addUnAvailableProperty(String name) {
        if(unAvailableProps == null) {
            throw new IllegalStateException("unAvailableProps is already nullified. Cannot call addUnAvailableProperty at this point");
        }
        unAvailableProps.add(name);
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getBooleanArrays()
     */
    public Map<String, Boolean[]> getBooleanArrays() {
        if(booleanArrays == null) {
            return Collections.emptyMap();
        }
        return this.booleanArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getBooleans()
     */
    public Map<String, Boolean> getBooleans() {
        if(booleans == null) {
            return Collections.emptyMap();
        }
        return this.booleans;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getCalendarArrays()
     */
    public Map<String, Calendar[]> getCalendarArrays() {
        if(calendarArrays == null) {
            return Collections.emptyMap();
        }
        return this.calendarArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getCalendars()
     */
    public Map<String, Calendar> getCalendars() {
        if(calendars == null) {
            return Collections.emptyMap();
        }
        return this.calendars;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getDoubleArrays()
     */
    public Map<String, Double[]> getDoubleArrays() {
        if(doubleArrays == null) {
            return Collections.emptyMap();
        }
        return this.doubleArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getDoubles()
     */
    public Map<String, Double> getDoubles() {
        if(doubles == null) {
            return Collections.emptyMap();
        }
        return this.doubles;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getLongArrays()
     */
    public Map<String, Long[]> getLongArrays() {
        if(longArrays == null) {
            return Collections.emptyMap();
        }
        return this.longArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getLongs()
     */
    public Map<String, Long> getLongs() {
        if(longs == null) {
            return Collections.emptyMap();
        }
        return this.longs;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getStringArrays()
     */
    public Map<String, String[]> getStringArrays() {
        if(stringArrays == null) {
            return Collections.emptyMap();
        }
        return this.stringArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getStrings()
     */
    public Map<String, String> getStrings() {
        if(strings == null) {
            return Collections.emptyMap();
        }
        return this.strings;
    }

    public Map<String, Object> getAllMapsCombined(){
        Map<String, Object> combined = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void putAll(Map<? extends String, ? extends Object> m) {
                if(m == null) {
                    return;
                }
                super.putAll(m);
            }
            
        };
        
        combined.putAll(this.booleanArrays);
        combined.putAll(this.booleans);
        
        // since Calendar object has setter, clone them
        if(calendarArrays != null) {
            for(Entry<String, Calendar[]> entry: this.calendarArrays.entrySet()) {
                combined.put(entry.getKey(), entry.getValue().clone());
            }
        }
        if(calendars != null) {
            for(Entry<String, Calendar> entry: this.calendars.entrySet()) {
                combined.put(entry.getKey(), entry.getValue().clone());
            }
        }
        
        combined.putAll(this.doubleArrays);
        combined.putAll(this.doubles);
        combined.putAll(this.longArrays);
        combined.putAll(this.longs);
        combined.putAll(this.stringArrays);
        combined.putAll(this.strings);
        return combined;
    }

    public void flush() {
       if(availableProps != null) {
         availableProps.clear();
       } 
       if(booleanArrays != null) {
          booleanArrays.clear();
       } 
       if(booleans != null) {
           booleans.clear();
       } 
       if(calendarArrays != null) {
           calendarArrays.clear();
       } 
       if(calendars != null) {
           calendars.clear();
       } 
       if(doubleArrays != null) {
           doubleArrays.clear();
       } 
       if(doubles != null) {
           doubles.clear();
       } 
       if(longArrays != null) {
           longArrays.clear();
       } 
       if(longs != null) {
           longs.clear();
       } 
       if(stringArrays != null) {
           stringArrays.clear();
       } 
       if(strings != null) {
           strings.clear();
       } 
    }

    /*
     * After the provider has been detached, it means that all HashMaps that are still EMPTY can be replaced
     * by null: This is much more memory efficient because there can be *many* HstNodeImpl's retained in the jvm memory
     */
    public void providerDetached() {
        if (booleans != null && booleans.isEmpty()) {
            booleans = null;
        }
        if(booleanArrays != null && booleanArrays.isEmpty()) {
            booleanArrays = null;
        }
        if(strings != null && strings.isEmpty()) {
            strings = null;
        }
        if(stringArrays != null && stringArrays.isEmpty()) {
            stringArrays = null;
        }
        if(doubles != null && doubles.isEmpty()) {
            doubles = null;
        }
        if(doubleArrays != null && doubleArrays.isEmpty()) {
            doubleArrays = null;
        }
        if(longs != null && longs.isEmpty()) {
            longs = null;
        }
        if(longArrays != null && longArrays.isEmpty()) {
            longArrays = null;
        }
        if(stringArrays != null && stringArrays.isEmpty()) {
            stringArrays = null;
        }
        if(calendars != null && calendars.isEmpty()) {
            calendars = null;
        }
        if(calendarArrays != null && calendarArrays.isEmpty()) {
            calendarArrays = null;
        }
        if(availableProps != null && availableProps.isEmpty()) {
            availableProps = null;
        }
        if(unAvailableProps != null && unAvailableProps.isEmpty()) {
            unAvailableProps = null;
        }
    }
    
}
