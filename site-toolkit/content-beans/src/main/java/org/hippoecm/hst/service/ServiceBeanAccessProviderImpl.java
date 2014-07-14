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
package org.hippoecm.hst.service;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Calendar;

import org.hippoecm.hst.provider.ValueProvider;

/**
 *
 * @deprecated since 2.28.05 (CMS 7.9.1). Do not use any more. No replacement
 */
@Deprecated
public class ServiceBeanAccessProviderImpl implements ServiceBeanAccessProvider, Serializable {
    
    private static final long serialVersionUID = 1L;

    public static final String HST_SERVICE_NAMESPACE_SEPARATOR = ":";
    
    protected Service service;
    protected ValueProvider valueProvider;
    
    public ServiceBeanAccessProviderImpl(Service service) throws IllegalAccessException, NoSuchFieldException {
        this.service = service;
        this.valueProvider = this.service.getValueProvider();
    }
    
    public Object getProperty(String namespacePrefix, String name, Class returnType, Method method) {
        if (UnderlyingServiceAware.class == method.getDeclaringClass()) {
            return this.service;
        }
        
        String nodePropName = (namespacePrefix != null ? namespacePrefix + HST_SERVICE_NAMESPACE_SEPARATOR + name : name);
        
        if (returnType != null && !returnType.isArray()) {
            if (returnType == String.class) {
                return this.valueProvider.getString(nodePropName);
            } else if (returnType == boolean.class || returnType == Boolean.class) {
                return this.valueProvider.getBoolean(nodePropName);
            } else if (returnType == long.class || returnType == Long.class) {
                return this.valueProvider.getLong(nodePropName);
            } else if (returnType == double.class || returnType == Double.class) {
                return this.valueProvider.getDouble(nodePropName);
            } else if (returnType == Calendar.class) {
                return this.valueProvider.getDate(nodePropName);
            }
        } else {
            if (returnType == String[].class) {
                return this.valueProvider.getStrings(nodePropName);
            } else if (returnType == boolean[].class || returnType == Boolean[].class) {
                return this.valueProvider.getBooleans(nodePropName);
            } else if (returnType == long[].class || returnType == Long[].class) {
                return this.valueProvider.getLongs(nodePropName);
            } else if (returnType == double[].class || returnType == Double[].class) {
                return this.valueProvider.getDoubles(nodePropName);
            } else if (returnType == Calendar[].class) {
                return this.valueProvider.getDates(nodePropName);
            }
        }
        
        return this.valueProvider.getProperties().get(nodePropName);
    }

    public Object setProperty(String namespacePrefix, String name, Object value, Class returnType, Method method) {
        if (UnderlyingServiceAware.class == method.getDeclaringClass()) {
            this.service = (Service) value;
            return null;
        }
        
        String nodePropName = (namespacePrefix != null ? namespacePrefix + HST_SERVICE_NAMESPACE_SEPARATOR + name : name);
        this.service.getValueProvider().getProperties().put(nodePropName, value);
        return null;
    }

    public Object invoke(String namespacePrefix, String name, Object[] args, Class returnType, Method method) {
        throw new UnsupportedOperationException("Service value provider does not support invocation on operations.");
    }

}
