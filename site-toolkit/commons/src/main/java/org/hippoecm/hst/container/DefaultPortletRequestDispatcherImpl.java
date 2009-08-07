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
 */package org.hippoecm.hst.container;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

public class DefaultPortletRequestDispatcherImpl implements HstPortletRequestDispatcherPathProvider {

    private static final String ATTRIBUTE_NAME = "hstPortletRequestDispatcherPathProvider.attributeName";
    private static final String PROPERTY_PATH = "hstPortletRequestDispatcherPathProvider.propertyPath";
    private static final String SERVLET_PATH_MAP = "hstPortletRequestDispatcherPathProvider.servletPathMap";

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private Map<String, PropertyDescriptor> propertyDescriptorCache;

    private String attributeName;
    private String propertyPath;
    private Map<Object, String> servletPathMap;
    
    public DefaultPortletRequestDispatcherImpl() {
        
    }

    public void init(PortletConfig config) throws PortletException {
        attributeName = config.getInitParameter(ATTRIBUTE_NAME);
        propertyPath = config.getInitParameter(PROPERTY_PATH);
        servletPathMap = new HashMap<Object, String>();

        String servletPathMapParam = config.getInitParameter(SERVLET_PATH_MAP);

        if (servletPathMapParam != null) {
            String[] items = servletPathMapParam.split(",");

            for (String item : items) {
                String[] pair = item.split("=");

                if (pair.length == 2) {
                    servletPathMap.put(pair[0].trim(), pair[1].trim());
                }
            }
        }
        
        propertyDescriptorCache = Collections.synchronizedMap(new HashMap<String, PropertyDescriptor>());
    }

    public String getServletPath(PortletRequest request) throws PortletException {
        String servletPath = null;
        Object bean = null;
        
        if (attributeName != null) {
            bean = request.getAttribute(attributeName);
            
            if (bean == null) {
                PortletSession portletSession = request.getPortletSession();
                
                if (portletSession != null) {
                    bean = portletSession.getAttribute(attributeName);
                    
                    if (bean == null) {
                        bean = portletSession.getAttribute(attributeName, PortletSession.APPLICATION_SCOPE);
                    }
                }
            }
        }
        
        if (bean != null) {
            try {
                Object value = getPropertyByPath(bean, propertyPath);
                
                if (value != null) {
                    servletPath = servletPathMap.get(value);
                }
            } catch (Exception e) {
                throw new PortletException(e);
            }
        }
        
        return servletPath;
    }

    public void destroy() {
    }
    
    private Object getPropertyByPath(Object bean, String path) throws Exception {
        Object value = bean;
        
        if (value != null && path != null && !"".equals(path.trim())) {
            String [] names = path.trim().split("\\.");
            
            for (String name : names) {
                if (value != null) {
                    value = getProperty(value, name);
                } else {
                    break;
                }
            }
        }
        
        return value;
    }
    
    private Object getProperty(Object bean, String name) throws Exception {
        PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);

        if (descriptor == null) {
            throw new NoSuchMethodException("Unknown property '" + name + "' on class '" + bean.getClass() + "'");
        }

        Method readMethod = descriptor.getReadMethod();

        if (readMethod == null) {
            throw new NoSuchMethodException("Property '" + name + "' has no getter method in class '" + bean.getClass() + "'");
        }

        Object value = readMethod.invoke(bean, EMPTY_OBJECT_ARRAY);
        
        return value;
    }
    
    private PropertyDescriptor getPropertyDescriptor(Object bean, String name) throws Exception {
        String propertyDescriptorKey = bean.getClass().getName() + "#" + name;
        PropertyDescriptor descriptor = propertyDescriptorCache.get(propertyDescriptorKey);
        
        if (descriptor == null) {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor [] descriptors = beanInfo.getPropertyDescriptors();
            
            for (PropertyDescriptor desc : descriptors) {
                if (desc.getName().equals(name)) {
                    descriptor = desc;
                    propertyDescriptorCache.put(propertyDescriptorKey, descriptor);
                    break;
                }
            }
        }
        
        return descriptor;
    }
    
}
