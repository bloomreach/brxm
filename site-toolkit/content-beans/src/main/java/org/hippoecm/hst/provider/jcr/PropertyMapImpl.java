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
        return availableProps.contains(name);
    }
    
    public void addAvailableProperty(String name) {
         availableProps.add(name);
    }
    
    public boolean isUnAvailableProperty(String name) {
        return unAvailableProps.contains(name);
    }
    
    public void addUnAvailableProperty(String name) {
         unAvailableProps.add(name);
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getBooleanArrays()
     */
    public Map<String, Boolean[]> getBooleanArrays() {

        return this.booleanArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getBooleans()
     */
    public Map<String, Boolean> getBooleans() {

        return this.booleans;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getCalendarArrays()
     */
    public Map<String, Calendar[]> getCalendarArrays() {

        return this.calendarArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getCalendars()
     */
    public Map<String, Calendar> getCalendars() {

        return this.calendars;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getDoubleArrays()
     */
    public Map<String, Double[]> getDoubleArrays() {

        return this.doubleArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getDoubles()
     */
    public Map<String, Double> getDoubles() {

        return this.doubles;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getLongArrays()
     */
    public Map<String, Long[]> getLongArrays() {

        return this.longArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getLongs()
     */
    public Map<String, Long> getLongs() {

        return this.longs;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getStringArrays()
     */
    public Map<String, String[]> getStringArrays() {

        return this.stringArrays;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.provider.jcr.P#getStrings()
     */
    public Map<String, String> getStrings() {

        return this.strings;
    }

    public Map<String, Object> getAllMapsCombined(){
        Map<String, Object> combined = new HashMap<String, Object>();
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
       this.availableProps.clear();
       this.booleanArrays.clear();
       this.booleans.clear();
       this.calendarArrays.clear();
       this.calendars.clear();
       this.doubleArrays.clear();
       this.doubles.clear();
       this.longArrays.clear();
       this.longs.clear();
       this.stringArrays.clear();
       this.strings.clear();
    }
    
}
