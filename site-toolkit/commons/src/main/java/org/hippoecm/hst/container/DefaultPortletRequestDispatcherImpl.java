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
package org.hippoecm.hst.container;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.hst.util.PortletConfigUtils;

/**
 * DefaultPortletRequestDispatcherImpl
 * 
 * @version $Id$
 */
public class DefaultPortletRequestDispatcherImpl implements HstPortletRequestDispatcherPathProvider {

    private static final String SERVLET_PATH_ATTRIBUTE_NAMES = "hstPortletRequestDispatcherPathProvider.servletPath.attributeNames";
    private static final String SERVLET_PATH_PROPERTY_PATHS = "hstPortletRequestDispatcherPathProvider.servletPath.propertyPaths";
    private static final String SERVLET_PATH_MAP = "hstPortletRequestDispatcherPathProvider.servletPathMap";

    private static final String PATH_INFO_ATTRIBUTE_NAMES = "hstPortletRequestDispatcherPathProvider.pathInfo.attributeNames";
    private static final String PATH_INFO_PROPERTY_PATHS = "hstPortletRequestDispatcherPathProvider.pathInfo.propertyPaths";
    private static final String PATH_INFO_PATTERN_REPLACEMENTS = "hstPortletRequestDispatcherPathProvider.pathInfo.patternReplacements";
    
    private static final String PAIR_SEPARATOR = "hstPortletRequestDispatcherPathProvider.pair.separator";
    private static final String PAIR_KEY_SEPARATOR = "hstPortletRequestDispatcherPathProvider.pair.key.separator";
    
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private Map<String, PropertyDescriptor> propertyDescriptorCache;

    private String [] servletPathAttributeNames;
    private String [] servletPathPropertyPaths;
    
    private String [] pathInfoAttributeNames;
    private String [] pathInfoPropertyPaths;
    
    private List<KeyValue<Pattern, String>> patternAndServletPaths;
    private List<KeyValue<Pattern, String>> pathInfoPatternAndReplacements;
    
    private String pairSeparator = ",";
    private String pairKeySeparator = "=";
    
    public DefaultPortletRequestDispatcherImpl() {
        
    }

    public void init(PortletConfig config) throws PortletException {
        pairSeparator = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), PAIR_SEPARATOR, pairSeparator);
        pairKeySeparator = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), PAIR_KEY_SEPARATOR, pairKeySeparator);
        
        servletPathAttributeNames = splitAndTrimToArray(PortletConfigUtils.getInitParameter(config, config.getPortletContext(), SERVLET_PATH_ATTRIBUTE_NAMES, null), pairSeparator);
        servletPathPropertyPaths = splitAndTrimToArray(PortletConfigUtils.getInitParameter(config, config.getPortletContext(), SERVLET_PATH_PROPERTY_PATHS, null), pairSeparator);
        pathInfoAttributeNames = splitAndTrimToArray(PortletConfigUtils.getInitParameter(config, config.getPortletContext(), PATH_INFO_ATTRIBUTE_NAMES, null), pairSeparator);
        pathInfoPropertyPaths = splitAndTrimToArray(PortletConfigUtils.getInitParameter(config, config.getPortletContext(), PATH_INFO_PROPERTY_PATHS, null), pairSeparator);
        
        patternAndServletPaths = new ArrayList<KeyValue<Pattern, String>>();
        String servletPathMapParam = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), SERVLET_PATH_MAP, null);
        
        if (servletPathMapParam != null) {
            String[] items = servletPathMapParam.split(pairSeparator);

            for (String item : items) {
                String [] pair = item.split(pairKeySeparator);

                if (pair.length >= 2) {
                    patternAndServletPaths.add(new DefaultKeyValue<Pattern, String>(Pattern.compile(pair[0].trim()), pair[1].trim()));
                }
            }
        }
        
        pathInfoPatternAndReplacements = new ArrayList<KeyValue<Pattern, String>>();
        String pathInfoPatternMapping = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), PATH_INFO_PATTERN_REPLACEMENTS, null);
        
        if (pathInfoPatternMapping != null) {
            String[] items = pathInfoPatternMapping.split(pairSeparator);

            for (String item : items) {
                String [] pair = item.split(pairKeySeparator);

                if (pair.length >= 2) {
                    pathInfoPatternAndReplacements.add(new DefaultKeyValue<Pattern, String>(Pattern.compile(pair[0].trim()), pair[1].trim()));
                }
            }
        }
        
        propertyDescriptorCache = Collections.synchronizedMap(new HashMap<String, PropertyDescriptor>());
    }

    public String getServletPath(PortletRequest request) throws PortletException {
        if (patternAndServletPaths.isEmpty()) {
            return null;
        }
        
        String servletPath = null;
        Object bean = null;
        
        if (servletPathAttributeNames != null && servletPathAttributeNames.length > 0) {
            for (int i = 0; i < servletPathAttributeNames.length && servletPath == null; i++) {
                String servletPathAttributeName = servletPathAttributeNames[i];
                String servletPathPropertyPath = (servletPathPropertyPaths.length > i ? servletPathPropertyPaths[i] : null);
                
                bean = request.getAttribute(servletPathAttributeName);
                
                if (bean == null) {
                    PortletSession portletSession = request.getPortletSession();
                    
                    if (portletSession != null) {
                        bean = portletSession.getAttribute(servletPathAttributeName);
                        
                        if (bean == null) {
                            bean = portletSession.getAttribute(servletPathAttributeName, PortletSession.APPLICATION_SCOPE);
                        }
                    }
                }
                
                if (bean != null) {
                    try {
                        Object value = getPropertyByPath(bean, servletPathPropertyPath);
                        
                        if (value != null) {
                            String valueString = value.toString();
                            
                            for (KeyValue<Pattern, String> pair : patternAndServletPaths) {
                                Matcher matcher = pair.getKey().matcher(valueString);
                                
                                if (matcher.matches()) {
                                    servletPath = pair.getValue();
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new PortletException(e);
                    }
                }
            }
        }
        
        return servletPath;
    }
    
    public String getPathInfo(PortletRequest request) throws PortletException {
        //TODO: For now, if it is not render phase, just return null to not use container provided path info.
        //      For example, in action phase, the request action parameters should be cared by the container provided path info.
        String lifecyclePhase = (String) request.getAttribute(PortletRequest.LIFECYCLE_PHASE);
        if (!PortletRequest.RENDER_PHASE.equals(lifecyclePhase)) {
            return null;
        }
        
        //TODO: For now, if it is not view mode, just return null to not use container provided path info.
        //      For example, in edit mode, the dedicated hst site map item should care the request.
        if (!PortletMode.VIEW.equals(request.getPortletMode())) {
            return null;
        }
        
        String pathInfo = null;
        Object bean = null;
        
        if (pathInfoAttributeNames != null && pathInfoAttributeNames.length > 0) {
            for (int i = 0; i < pathInfoAttributeNames.length && pathInfo == null; i++) {
                String pathInfoAttributeName = pathInfoAttributeNames[i];
                String pathInfoPropertyPath = (pathInfoPropertyPaths.length > i ? pathInfoPropertyPaths[i] : null);
                
                bean = request.getAttribute(pathInfoAttributeName);
                
                if (bean == null) {
                    PortletSession portletSession = request.getPortletSession();
                    
                    if (portletSession != null) {
                        bean = portletSession.getAttribute(pathInfoAttributeName);
                        
                        if (bean == null) {
                            bean = portletSession.getAttribute(pathInfoAttributeName, PortletSession.APPLICATION_SCOPE);
                        }
                    }
                }
                
                if (bean != null) {
                    try {
                        pathInfo = (String) getPropertyByPath(bean, pathInfoPropertyPath);
                    } catch (Exception e) {
                        throw new PortletException(e);
                    }
                }
                
                if (pathInfo != null && !pathInfoPatternAndReplacements.isEmpty()) {
                    for (KeyValue<Pattern, String> pair : pathInfoPatternAndReplacements) {
                        Matcher matcher = pair.getKey().matcher(pathInfo);
                        
                        if (matcher.find()) {
                            pathInfo = matcher.replaceAll(pair.getValue());
                            break;
                        }
                    }
                }
            }
        }
        
        return pathInfo;
    }

    public void destroy() {
        propertyDescriptorCache.clear();
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
    
    private static String [] splitAndTrimToArray(String value, String regex) {
        if (value == null) {
            return new String[0];
        }
        
        String [] values = value.split(regex);
        
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        
        return values;
    }
    
}
