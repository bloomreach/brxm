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
package org.hippoecm.hst.core.request;

import java.util.Map;

import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;

/**
 * A <code>SiteMapItemHandlerConfiguration</code> is the runtime instance of a {@link HstSiteMapItemHandlerConfiguration} and provides some configuration information to a sitemap item handler.
 * 
 */
public interface SiteMapItemHandlerConfiguration {

	/**
	 * @param <T>
	 * @param name
	 * @param resolvedSiteMapItem
	 * @param mappingClass the class the property value should be of
	 * @return  the property if it is of type mappingClass. If the value is a String or String[] and contains a property placeholder it is resolved with the help of the ResolvedSiteMapItem. If there is a property placeholder
     * that cannot be resolved, <code>null</code> will be returned in case for a String and in case of a String[] the value of the item in the array will be <code>null</code>
	 */
    <T> T getProperty(String name, ResolvedSiteMapItem resolvedSiteMapItem, Class<T> mappingClass);
    
    /**
     * Returns all resolved properties of type mappingClass into a map. If the property is of type String and contains a property placeholder, this property placeholder
     * is replaced with it's value. If it cannot be resolved, the property won't be in the map. If the property is of type String[], all properties having a property placeholder
     * are resolved. If a property placeholder cannot be resolved, that item in the array will be <code>null</code>
     * @param <T>
     * @param hstResolvedSiteMapItem
     * @param mappingClass the class the property values should be of
     * @return  Returns the map of all resolved properties of type T into a map and an empty map if there are no properties of type T or there are only properties
     * with unresolvable placeholders
     */
    <T> Map<String, T> getProperties(ResolvedSiteMapItem resolvedSiteMapItem, Class<T> mappingClass);
    
    /**
     * Returns the unresolved property for <code>name</code> or <code>null</code> when not present or is not of type mappingClass
     * @param <T>
     * @param name
     * @param mappingClass the class the property value should be of
     * @return  Returns the unresolved property for <code>name</code> or <code>null</code> when not present or not of type mappingClass
     */
    <T> T getRawProperty(String name, Class<T> mappingClass);
    
    /**
     * Returns the map of all unresolved properties of type T into a map and an empty map if there are no properties of type mappingClass
     * @param <T>
     * @param mappingClass the class the property values should be of
     * @return  Returns the map of all unresolved properties of type T into a map and an empty map if there are no properties of type mappingClass
     */
    <T> Map<String, T> getRawProperties(Class<T> mappingClass);
    
}
