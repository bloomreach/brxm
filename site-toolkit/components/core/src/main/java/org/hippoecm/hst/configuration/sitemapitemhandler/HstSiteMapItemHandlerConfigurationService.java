/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.sitemapitemhandler;

import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.internal.StringPool;

public class HstSiteMapItemHandlerConfigurationService implements HstSiteMapItemHandlerConfiguration {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String sitemapItemHandlerClassName;
    private Map<String, Object> properties;
     
    public HstSiteMapItemHandlerConfigurationService(HstNode handleNode) throws ModelLoadingException {
        id = StringPool.get(handleNode.getValueProvider().getName());
        name = StringPool.get(handleNode.getValueProvider().getName());
        sitemapItemHandlerClassName = handleNode.getValueProvider().getString(HstNodeTypes.SITEMAPITEMHANDLDER_PROPERTY_CLASSNAME);
        if(sitemapItemHandlerClassName == null || "".equals(sitemapItemHandlerClassName)) {
            throw new ModelLoadingException("Invalid sitemap item handler because property '"+HstNodeTypes.SITEMAPITEMHANDLDER_PROPERTY_CLASSNAME+"' is missing or empty ");
        }
        sitemapItemHandlerClassName = StringPool.get(sitemapItemHandlerClassName.trim());
        
        properties = handleNode.getValueProvider().getProperties();
        
    }
    public String getSiteMapItemHandlerClassName() {
        return sitemapItemHandlerClassName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a property of type String, Boolean, Long, Double or Calendar or an array of one of these objects or <code>null</code> when not present
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Returns the map of property names to their value. The value can be of type String, Boolean, Long, Double or Calendar or an array of one of these objects
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

}
