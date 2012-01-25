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
package org.hippoecm.hst.core.util;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * PropertyParser
 * 
 * @version $Id$
 */
public class PropertyParser {

    public final static String DEFAULT_PLACEHOLDER_PREFIX = "${";
    public final static String DEFAULT_PLACEHOLDER_SUFFIX = "}";
    public final static PropertyPlaceholderHelper PROPERTY_PLACE_HOLDER_HELPER = new PropertyPlaceholderHelper(DEFAULT_PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_SUFFIX, null, false);
    
    private final static Logger log = LoggerFactory.getLogger(PropertyParser.class);
    private Properties properties;
    
    public PropertyParser(Properties properties){
        this.properties = properties;
        // we want an expeption for unresolved properties, this use 'false'
    }
    
    public Object resolveProperty(String name, Object o) {
        if(o == null || properties == null) {
            return o;
        }
        
        if(o instanceof String) {
            String s = (String)o;
            // replace possible expressions
            try {
              s =  PROPERTY_PLACE_HOLDER_HELPER.replacePlaceholders((String)o, properties);
            } catch (IllegalArgumentException e) {
              log.debug("Unable to replace property expression for property '{}'. Return null : '{}'" ,name, e); 
              return null;
              
            }
            return s;
        }
        if(o instanceof String[]) {
            // replace possible expressions in every String
            String[] unparsed = (String[])o;
            String[] parsed = new String[unparsed.length];
            for(int i = 0 ; i < unparsed.length ; i++) {
                String s = unparsed[i];
                try {
                    s =  PROPERTY_PLACE_HOLDER_HELPER.replacePlaceholders(unparsed[i], properties);
                } catch (IllegalArgumentException e ) {
                    log.debug("Unable to replace property expression for property '{}'. Return null : '{}'.",name, e);
                    s = null;
                }
                parsed[i] = s;
            }
            
            return parsed;
        }
        return o;
    }
    
}


