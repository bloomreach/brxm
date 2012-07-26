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

package org.hippoecm.frontend.plugins.yui.util;

import java.util.HashMap;
import java.util.Map;

//TODO: remove

@Deprecated
public class OptionsUtil {

    public static final String KEY_VALUE_DELIM = "=";

    @Deprecated
    public static void addKeyValuePairsToMap(Map<String, String> map, String... pairs) {
        for(String option : pairs) {
            int delimIndex = option.indexOf(KEY_VALUE_DELIM);
            if( delimIndex == -1 ) {
                throw new IllegalArgumentException("No delimiter[" + KEY_VALUE_DELIM + "] found in option [" + option + "]");
            }
            map.put(option.substring(0, delimIndex), option.substring(delimIndex+1));
        }
    }

    @Deprecated
    public static Map<String, String> keyValuePairsToMap(String... pairs) {
        Map<String, String> map = new HashMap<String, String>();
        addKeyValuePairsToMap(map, pairs);
        return map;
    }
}
