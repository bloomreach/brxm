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

import java.util.Properties;
import java.util.Set;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
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
     * @return the {@link ResolvedMount} for this resolvedSiteMapItem instance
     */
    ResolvedMount getResolvedMount();
    
    /**
     * This method returns a content path, relative to the {@link Mount#getContentPath()}. This value should 
     * have resolved property placeholders, like ${1}/${2}. If a property placeholder cannot be resolved, the implementation may return <code>null</code>
     * @return the content path relative to the {@link Mount#getContentPath()} or <code>null</code> if not present or has unresolvable property placeholders
     */
    String getRelativeContentPath();
    
    /**
     * Returns a relative path from hst request path to the SiteMapItem that was matched. This path <b>never</b> starts with a "/".
     * @return the matched path to this <code>ResolvedSiteMapItem</code>, relative to the mount path
     */
    String getPathInfo();

    /**
     * @return the page title for this {@link ResolvedSiteMapItem} or <code>null</code> if not configured or configured incorrect:
     * An incorrect configuration is the case when there are property placeholders used for wildcards that cannot be resolved.
     */
    String getPageTitle();
    
    /**
     * Returns a property from the HstSiteMapItem configuration but should have replaced possible property placeholders. If a property 
     * placeholder cannot be resolved, the implementation can return <code>null</code>. Parameters are inherited from ancestor items,
     * but in case of the same name, ancestor items have a lower precedence
     * 
     * @param name the name of the parameter
     * @return the value of the parameter and <code>null</code> if the parameter is not there or a property placeholder cannot be resolved
     */
    String getParameter(String name);
    
    /**
     * See {@link #getParameter(String)} only without inheritance of ancestor items
     * @param name the name of the parameter
     * @return the value of the parameter and <code>null</code> if the parameter is not there or a property placeholder cannot be resolved
     */
    String getLocalParameter(String name);
    
    /**
     * Return the parameter map from the HstSiteMapItem configuration, but all values containing  property placeholders should be resolved.
     * Parameters are inherited from ancestor items, but in case of the same name, ancestor items have a lower precedence
     * @return the all the parameters as a Properties map, and an empty Properties object when no parameters defined or inherited, or the parameter values cannot be resolved
     */
    Properties getParameters();
    
    /**
     * See {@link #getParameters()} only without inheritance of ancestor items
     * @return the all the parameters as a Properties map, and an empty Properties object when no parameters defined, or the parameter values cannot be resolved
     */
    Properties getLocalParameters();
    
    /**
     * @return the <code>HstSiteMapItem</code> that is matched for this request
     */
    HstSiteMapItem getHstSiteMapItem();
    
    /**
     * Returns the <code>namedPipeline</code> to be used for the Hst Request Processing. When the backing {@link HstSiteMapItem} does not contain one,
     * possibly, when present, it is inherited from its backing {@link Mount}. If this one does not have it either, <code>null</code> is returned
     * implying the default pipeline will be called
     * 
     * @return the <code>namedPipeline</code> for this ResolvedSiteMapItem or <code>null</code> implying the default should be used
     */
    String getNamedPipeline();
    
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
     * @return <code>true</code> if {@link #getRoles} should be applied to this ResolvedSiteMap item
     */
    boolean isAuthenticated();
    
    /**
     * @return the roles that are allowed to proceed the request with this resolved sitemap item. If no roles present, and empty list is returned
     */
    Set<String> getRoles();
    
    /**
     * @return the users that are allowed to proceed the request with this resolved sitemap item. If no users present, and empty list is returned
     */
    Set<String> getUsers();
    
    /**
     * @return the root <code>HstComponentConfiguration</code> that is configured on the backing {@link #getHstSiteMapItem()} of this ResolvedSiteMapItem
     */
    HstComponentConfiguration getHstComponentConfiguration();
}
