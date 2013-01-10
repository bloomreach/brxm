/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.util.PropertyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SiteMapItemHandlerConfigurationImpl which is the runtime instance of the SiteMapItemHandlerConfiguration. Note that 
 * this object is not thread-safe, and created only once for a {@link HstSiteMapItemHandler} and attached to it during init
 *
 */
public class SiteMapItemHandlerConfigurationImpl implements SiteMapItemHandlerConfiguration {

private final static Logger log = LoggerFactory.getLogger(ComponentConfigurationImpl.class);
    
    public HstSiteMapItemHandlerConfiguration handlerConfig;
    
    public SiteMapItemHandlerConfigurationImpl(HstSiteMapItemHandlerConfiguration handlerConfig) {
        this.handlerConfig = handlerConfig;
    }
    
    /**
     * <p>a property is of type String, Boolean, Long, Double or Calendar or an array of one of these objects.</p>
     * 
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getProperties(ResolvedSiteMapItem resolvedSiteMapItem, Class<T> mappingClass) {
        Map<String,T> properties = new HashMap<String, T>();
        PropertyParser pp = new PropertyParser(resolvedSiteMapItem.getParameters());
        for(Entry<String, Object> entry: handlerConfig.getProperties().entrySet()) {
            Object val = entry.getValue();
            if(val == null){ 
                log.debug("value is null for '{}'. Skip property", entry.getKey());
                continue;
            } 
            if(mappingClass.isAssignableFrom(val.getClass())){
                if(val instanceof String || val instanceof String[]) {
                    Object parsedVal = pp.resolveProperty(entry.getKey(), val);
                    if(parsedVal != null) {
                        properties.put(entry.getKey(), (T)parsedVal);
                    }
                } else {
                    properties.put(entry.getKey(), (T)val); 
                }
            } else {
                log.debug("Found a property for key '{}' but the value is not assignable from '{}' because it is of type '"+val.getClass().getName()+"'. Skip key-val pair.", entry.getKey(), mappingClass.getName());
            }
        }
        return properties;
    }

    /**
     * <p>a property is of type String, Boolean, Long, Double or Calendar or an array of one of these objects.</p>
     * 
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, ResolvedSiteMapItem resolvedSiteMapItem, Class<T> mappingClass) {
        Object val =  handlerConfig.getProperty(name);
        if(val == null){ 
            log.debug("value is null for '{}'. Return null");
            return null;
        } 
        PropertyParser pp = new PropertyParser(resolvedSiteMapItem.getParameters());
        if(mappingClass.isAssignableFrom(val.getClass())){
            if(val instanceof String || val instanceof String[]) {
                Object parsedVal = pp.resolveProperty(name, val);
                return  (T)parsedVal;
            } else {
                return (T)val;
            }
        } else {
            log.debug("Found a property for key '{}' but the value is not assignable from '{}' because it is of type '"+val.getClass().getName()+"'. Skip key-val pair.", name , mappingClass.getName());
            return null;
        }
    }
    
    /**
     * <p>a property is of type String, Boolean, Long, Double or Calendar or an array of one of these objects.</p>
     * 
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getRawProperties(Class<T> mappingClass) {
        Map<String,T> parameters = new HashMap<String, T>();
        for(Entry<String, Object> entry: handlerConfig.getProperties().entrySet()) {
            Object val = entry.getValue();
            if(val == null){ 
                log.debug("value is null for '{}'. Skip property", entry.getKey());
                continue;
            } 
            if(mappingClass.isAssignableFrom(val.getClass())){
               parameters.put(entry.getKey(), (T)val); 
            }
        }
        return parameters;
    }

    /**
     * <p>a property is of type String, Boolean, Long, Double or Calendar or an array of one of these objects.</p>
     * 
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T getRawProperty(String name, Class<T> mappingClass) {
        Object val =  handlerConfig.getProperty(name);
        if(mappingClass.isAssignableFrom(val.getClass())){
            return (T)val;
        } else {
            log.debug("Found a property for key '{}' but the value is not assignable from '{}' because it is of type '"+val.getClass().getName()+"'. Skip key-val pair.", name , mappingClass.getName());
            return null;
        }
    }

  
}
