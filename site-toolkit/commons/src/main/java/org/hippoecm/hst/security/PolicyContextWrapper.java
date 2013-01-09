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
package org.hippoecm.hst.security;

import java.lang.reflect.Method;
import java.util.Set;

import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;

/**
 * PolicyContextWrapper
 * <P>
 * Wrapper implementation for javax.security.jacc.PolicyContext
 * introduced by JACC (Java Authorization Contract for Containers).
 * Unfortunately, this is not provided by many servlet container.
 * So, this wrapper class will be used for future integration.
 * </P>
 * @version $Id$
 */
public class PolicyContextWrapper {
    
    private static Class<?> policyContextClazz;
    private static Method getContextMethod;
    private static Method getContextIDMethod;
    private static Method getHandlerKeysMethod;
    private static Method setContextIDMethod;
    private static Method setHandlerDataMethod;
    
    static {
        try {
            policyContextClazz = Class.forName("javax.security.jacc.PolicyContext");
        } catch (Throwable ignore) {
        }
        
        if (policyContextClazz != null) {
            try {
                getContextMethod = policyContextClazz.getMethod("getContext", String.class);
                getContextIDMethod = policyContextClazz.getMethod("getContextID");
                getHandlerKeysMethod = policyContextClazz.getMethod("getHandlerKeys");
                setContextIDMethod = policyContextClazz.getMethod("setContextID", String.class);
                setHandlerDataMethod = policyContextClazz.getMethod("setHandlerData", Object.class);
            } catch (Throwable th) {
                Logger logger = HstServices.getLogger("org.hippoecm.hst.security.PolicyContextWrapper");
                logger.warn("Failed to load methods of javax.security.jacc.PolicyContext", th);
            }
        }
    }
    
    private PolicyContextWrapper() {
        
    }
    
    public static boolean isAvailable() {
        return (policyContextClazz != null);
    }
    
    public static Object getContext(String key) {
        if (getContextMethod != null) {
            try {
                return getContextMethod.invoke(policyContextClazz, key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        return null;
    }
    
    public static String getContextID() {
        if (getContextIDMethod != null) {
            try {
                return (String) getContextIDMethod.invoke(policyContextClazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        return null;
    }
    
    public static Set getHandlerKeys() {
        if (getHandlerKeysMethod != null) {
            try {
                return (Set) getHandlerKeysMethod.invoke(policyContextClazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        return null;
    }
    
    public static void setContextID(String contextID) {
        if (setContextIDMethod != null) {
            try {
                setContextIDMethod.invoke(policyContextClazz, contextID);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public static void setHandlerData(Object data) {
        if (setHandlerDataMethod != null) {
            try {
                setHandlerDataMethod.invoke(policyContextClazz, data);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
