/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.provider.PropertyMap;

class PropertyMapImpl implements PropertyMap, Serializable {

    private static final long serialVersionUID = 1L;
    
    private Map<String, Boolean> booleans = Collections.emptyMap();
    private Map<String, Boolean[]> booleanArrays = Collections.emptyMap();

    private Map<String, String> strings = Collections.emptyMap();
    private Map<String, String[]> stringArrays = Collections.emptyMap();

    private Map<String, Double> doubles = Collections.emptyMap();
    private Map<String, Double[]> doubleArrays = Collections.emptyMap();

    private Map<String, Long> longs = Collections.emptyMap();
    private Map<String, Long[]> longArrays = Collections.emptyMap();

    private Map<String, Calendar> calendars = Collections.emptyMap();
    private Map<String, Calendar[]> calendarArrays = Collections.emptyMap();

    private Set<String> availableProps = Collections.emptySet();
    private Set<String> unAvailableProps = Collections.emptySet();
    
    boolean hasProperty(String name) {
        return availableProps.contains(name);
    }
    
    void addAvailableProperty(String name) {
        if(availableProps == Collections.EMPTY_SET) {
            availableProps = new HashSet<String>();
        }
        availableProps.add(name);
    }
    
    boolean isUnAvailableProperty(String name) {
        return unAvailableProps.contains(name);
    }
    
    void addUnAvailableProperty(String name) {
        if(unAvailableProps == Collections.EMPTY_SET) {
            unAvailableProps = new HashSet<String>();
        }
        unAvailableProps.add(name);
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getBooleanArrays()
     */
    public Map<String, Boolean[]> getBooleanArrays() {
        return this.booleanArrays;
    }

    void put(String name, Boolean[] booleanArray) {
        if (booleanArrays == Collections.EMPTY_MAP) {
            booleanArrays = new HashMap<String, Boolean[]>();
        }
        booleanArrays.put(name, booleanArray);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getBooleans()
     */
    public Map<String, Boolean> getBooleans() {
        return this.booleans;
    }

    void put(String name, Boolean b) {
        if (booleans == Collections.EMPTY_MAP) {
            booleans = new HashMap<String, Boolean>();
        }
        booleans.put(name, b);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getCalendarArrays()
     */
    public Map<String, Calendar[]> getCalendarArrays() {
        return this.calendarArrays;
    }

    void put(String name, Calendar[] calendarArray) {
        if (calendarArrays == Collections.EMPTY_MAP) {
            calendarArrays = new HashMap<String, Calendar[]>();
        }
        calendarArrays.put(name, calendarArray);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getCalendars()
     */
    public Map<String, Calendar> getCalendars() {
        return this.calendars;
    }

    void put(String name, Calendar calendar) {
        if (calendars == Collections.EMPTY_MAP) {
            calendars = new HashMap<String, Calendar>();
        }
        calendars.put(name, calendar);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getDoubleArrays()
     */
    public Map<String, Double[]> getDoubleArrays() {
        return this.doubleArrays;
    }

    void put(String name, Double[] doubleArray) {
        if (doubleArrays == Collections.EMPTY_MAP) {
            doubleArrays = new HashMap<String, Double[]>();
        }
        doubleArrays.put(name, doubleArray);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getDoubles()
     */
    public Map<String, Double> getDoubles() {
        return this.doubles;
    }

    void put(String name, Double d) {
        if (doubles == Collections.EMPTY_MAP) {
            doubles = new HashMap<String, Double>();
        }
        doubles.put(name, d);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getLongArrays()
     */
    public Map<String, Long[]> getLongArrays() {
        return this.longArrays;
    }

    void put(String name, Long[] longArray) {
        if (longArrays == Collections.EMPTY_MAP) {
            longArrays = new HashMap<String, Long[]>();
        }
        longArrays.put(name, longArray);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getLongs()
     */
    public Map<String, Long> getLongs() {
        return this.longs;
    }

    void put(String name, Long l) {
        if (longs == Collections.EMPTY_MAP) {
            longs = new HashMap<String, Long>();
        }
        longs.put(name, l);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getStringArrays()
     */
    public Map<String, String[]> getStringArrays() {
        return this.stringArrays;
    }

    void put(String name, String[] stringArray) {
        if (stringArrays == Collections.EMPTY_MAP) {
            stringArrays = new HashMap<String, String[]>();
        }
        stringArrays.put(name, stringArray);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getStrings()
     */
    public Map<String, String> getStrings() {
        return this.strings;
    }

    void put(String name, String s) {
        if (strings == Collections.EMPTY_MAP) {
            strings = new HashMap<String, String>();
        }
        strings.put(name, s);
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
        for(Entry<String, Calendar[]> entry: this.calendarArrays.entrySet()) {
           combined.put(entry.getKey(), entry.getValue().clone());
        }

        for(Entry<String, Calendar> entry: this.calendars.entrySet()) {
            combined.put(entry.getKey(), entry.getValue().clone());
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
        availableProps.clear();
        booleanArrays.clear();
        booleans.clear();
        calendarArrays.clear();
        calendars.clear();
        doubleArrays.clear();
        doubles.clear();
        longArrays.clear();
        longs.clear();
        stringArrays.clear();
        strings.clear();
    }

    /*
     * After the provider has been detached, it means that all HashMaps that are sparse can be optimized.
     */
    void providerDetached() {
        booleans = CollectionOptimizer.optimizeHashMap(booleans);
        booleanArrays = CollectionOptimizer.optimizeHashMap(booleanArrays);
        strings = CollectionOptimizer.optimizeHashMap(strings);
        stringArrays = CollectionOptimizer.optimizeHashMap(stringArrays);
        doubles = CollectionOptimizer.optimizeHashMap(doubles);
        doubleArrays = CollectionOptimizer.optimizeHashMap(doubleArrays);
        longs = CollectionOptimizer.optimizeHashMap(longs);
        longArrays = CollectionOptimizer.optimizeHashMap(longArrays);
        calendars = CollectionOptimizer.optimizeHashMap(calendars);
        calendarArrays = CollectionOptimizer.optimizeHashMap(calendarArrays);
        availableProps = CollectionOptimizer.optimizeHashSet(availableProps);
        unAvailableProps = CollectionOptimizer.optimizeHashSet(unAvailableProps);
    }

}
