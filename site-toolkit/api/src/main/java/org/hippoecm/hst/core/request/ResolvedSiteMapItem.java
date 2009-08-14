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
package org.hippoecm.hst.core.request;

import java.util.List;
import java.util.Properties;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * An instance of the implementation of this interface will be available on the <code>{@link HstRequestContext}</code>. It has a reference
 * to the <code>{@link HstSiteMapItem}</code> that matched the request, and a reference to the corresponding <code>{@link HstComponentConfiguration}</code>.
 * <p/>
 * Just as the <code>{@link HstSiteMapItem}</code>, this interface has the same methods <code>{@link #getParameters()}</code> and <code>{@link #getParameter(String)}</code>.
 * A ResolvedSiteMapItem implementation though does not have to return a unmodifiable map for <code>{@link #getParameters()}</code> as opposed to 
 * the one from {@link HstSiteMapItem#getParameters()}. Furthermore, typically a ResolvedSiteMapItem implementation returns the same
 * parameters as where on the HstSiteMapItem available, but resolves any property placeholders.
 * <p/>
 * For example, if the {@link HstSiteMapItem} that was used to create this <code>ResolvedSiteMapItem</code> instance, had a property placeholder
 * in a parameter like : <code>foo = ${1}</code>, then the <code>ResolvedSiteMapItem</code> instance might have replaced <code>${1}</code> with something
 * from the pathInfo. The <code>ResolvedSiteMapItem</code> is allowed to return <code>null</code> values for parameters that do exist.
 */
public interface ResolvedSiteMapItem {
    
    /**
     * This method returns a content path, relative to the {@link org.hippoecm.hst.configuration.HstSite#getContentPath()}. This value should 
     * have resolved property placeholders, like ${1}/${2}. If a property placeholder cannot be resolved, the implementation may return <code>null</code>
     * @return the content path relative to the {@link org.hippoecm.hst.configuration.HstSite#getContentPath()} or <code>null</code> if not present or has unresolvable property placeholders
     */
    String getRelativeContentPath();
    
    /**
     * Returns a relative path wrt servlet path to the SiteMapItem that was matched
     * @return the matched path to this <code>ResolvedSiteMapItem</code>, relative to the servletpath
     */
    String getPathInfo();
    
    /**
     * Returns a property from the HstSiteMapItem configuration but should have replaced possible property placeholders. If a property 
     * placeholder cannot be resolved, the implementation can return <code>null</code>. 
     * 
     * @param name the name of the parameter
     * @return the value of the parameter and <code>null</code> if the parameter is not there or a property placeholder cannot be resolved
     */
    String getParameter(String name);
    
    /**
     * Return the parameter map from the HstSiteMapItem configuration, but all values containing  property placeholders should be resolved
     * @return the all the parameters as a Properties map, where all values containing property placeholders should their properties have resolved
     */
    Properties getParameters();
    
    /**
     * @return the <code>HstSiteMapItem</code> that is matched for this request
     */
    HstSiteMapItem getHstSiteMapItem();
    
    /**
     * 
     * @return the statusCode specified by <code>{@link HstSiteMapItem#getStatusCode()}</code> 
     */
    int getStatusCode();
    
    /**
     * 
     * @return the errorCode specified by <code>{@link HstSiteMapItem#getErrorCode()}</code> 
     */
    int getErrorCode();
    
    /**
     * @return the roles that are allowed to proceed the request with this resolved sitemap item. If no roles present, and empty list is returned
     */
    List<String> getRoles();
    
    /**
     * @return <code>true</code> if {@link #getRoles} should be applied to this ResolvedSiteMap item
     */
    boolean isSecured();
    
    /**
     * @return the <code>HstComponentConfiguration</code> that is found through {@link #getHstSiteMapItem()}
     */
    HstComponentConfiguration getHstComponentConfiguration();
    
    /**
     * @return the <code>HstComponentConfiguration</code> for portlet that is found through {@link #getHstSiteMapItem()}
     */
    HstComponentConfiguration getPortletHstComponentConfiguration();
  
}
