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
package org.hippoecm.hst.provider;

import java.util.Calendar;
import java.util.Map;

public interface PropertyMap {

    Map<String, Boolean[]> getBooleanArrays();

    Map<String, Boolean> getBooleans();

    Map<String, Calendar[]> getCalendarArrays();

    Map<String, Calendar> getCalendars();

    Map<String, Double[]> getDoubleArrays();

    Map<String, Double> getDoubles();

    Map<String, Long[]> getLongArrays();

    Map<String, Long> getLongs();

    Map<String, String[]> getStringArrays();

    Map<String, String> getStrings();

    Map<String, Object> getAllMapsCombined();
    
    void flush();
}