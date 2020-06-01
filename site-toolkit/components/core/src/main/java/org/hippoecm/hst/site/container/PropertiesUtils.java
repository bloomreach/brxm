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
package org.hippoecm.hst.site.container;

import java.util.Enumeration;
import java.util.Properties;

/**
 * PropertiesUtils
 * @version $Id$
 */
public class PropertiesUtils {
    
    private PropertiesUtils() {
        
    }
    
    public static void copyToSystem(Properties source) {
        copy(source, System.getProperties());
    }
    
    public static void copy(Properties source, Properties target) {
        for (Enumeration<?> enumNames = source.propertyNames(); enumNames.hasMoreElements(); ) {
            String key = (String) enumNames.nextElement();
            String value = source.getProperty(key);
            
            if (value != null) {
                target.setProperty(key, value);
            }
        }
    }
    
}
