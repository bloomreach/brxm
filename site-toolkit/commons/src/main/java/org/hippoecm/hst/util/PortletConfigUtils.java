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
package org.hippoecm.hst.util;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

/**
 * PortletConfigUtils
 * 
 * @version $Id$
 */
public class PortletConfigUtils {
    
    private PortletConfigUtils() {
        
    }
    
    /**
     * Retrieves the init parameter from the portletConfig or portletContext.
     * If the init parameter is not found in portletConfig, then it will look up the init parameter from the portletContext.
     * If either servletConfig or servletContext is null, then either is not used to look up the init parameter.
     * If the parameter is not found, then it will return the defaultValue.
     * @param portletConfig portletConfig. If null, this is not used.
     * @param portletContext portletContext. If null, this is not used.
     * @param paramName parameter name
     * @param defaultValue the default value
     * @return
     */
    public static String getInitParameter(PortletConfig portletConfig, PortletContext portletContext, String paramName, String defaultValue) {
        String value = null;
        
        if (value == null && portletConfig != null) {
            value = portletConfig.getInitParameter(paramName);
        }
        
        if (value == null && portletContext != null) {
            value = portletContext.getInitParameter(paramName);
        }
        
        if (value == null) {
            value = defaultValue;
        }
        
        return value;
    }
    
    /**
     * @see {@link ServletConfigUtils#getInitParameter(String, String, Object...)}
     * @param paramName
     * @param defaultValue
     * @param configs
     * @return
     */
    public static String getInitParameter(String paramName, String defaultValue, Object ... configs) {
        return ServletConfigUtils.getInitParameter(paramName, defaultValue, configs);
    }
    
    /**
     * @see {@link ServletConfigUtils#isEmpty(String)}
     * @param s
     * @return
     */
    public static boolean isEmpty(String s) {
        return ServletConfigUtils.isEmpty(s);
    }
    
    /**
     * @see {@link ServletConfigUtils#isBlank(String)}
     * @param s
     * @return
     */
    public static boolean isBlank(String s) {
        return ServletConfigUtils.isBlank(s);
    }
    
}
