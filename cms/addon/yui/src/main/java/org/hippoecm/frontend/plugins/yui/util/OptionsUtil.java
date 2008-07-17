package org.hippoecm.frontend.plugins.yui.util;

import java.util.HashMap;
import java.util.Map;

public class OptionsUtil {
    
    public static final String KEY_VALUE_DELIM = "=";

    public static void addKeyValuePairsToMap(Map<String, String> map, String... pairs) {
        for(String option : pairs) {
            int delimIndex = option.indexOf(KEY_VALUE_DELIM); 
            if( delimIndex == -1 )
                throw new IllegalArgumentException("No delimiter[" + KEY_VALUE_DELIM + "] found in option [" + option + "]");
            map.put(option.substring(0, delimIndex), option.substring(delimIndex+1));
        }
    }
    
    public static Map<String, String> keyValuePairsToMap(String... pairs) {
        Map<String, String> map = new HashMap<String, String>();
        addKeyValuePairsToMap(map, pairs);
        return map;
    }
}
