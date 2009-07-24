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
package org.hippoecm.hst.configuration.sitemap;

import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
 
/**
 * A <code>HstSiteMapItem<code> is used as a representation of a logical path (element) for matching (part of a) external URL 
 * to a repository content location. Through its id it can be directly accessed by the {@link HstSiteMap}, to for example
 * create a link to it. Obviously, creating a link to a <code>HstSiteMapItem</code> does only make sense when its path in the 
 * <code>HstSiteMap</code> does not contain WILDCARD's ( <code>*</code> or <code>**</code>) 
 *
 * <p/>
 * NOTE: As {@link HstComponent} instances can access <code>HstSiteMapItem</code> instances but should not be able to modify them, 
 * implementations must make sure that through the api a <code>HstSiteMapItem</code> instance cannot be changed. Returned List and Map
 * should be therefor unmodifiable. 
 */

public interface HstSiteMapItem {

    /**
     * The id of a <code>HstSiteMapItem</code> is mandatory and must be unique within its containing <code>{@link HstSiteMap}</code> because
     * <code>{@link HstSiteMap#getSiteMapItemById(String)}</code> must uniquely return a <code>HstSiteMapItem</code>
     * @return the id of this HstSiteMapItem
     */
    String getId(); 
    
    /**
     * Returns the logical path element of this <code>SiteMapItem</code>. The constraint to the return value is, that it needs to be
     * unique within the sibbling <code>HstSiteMapItem</code>'s because it is used as a key for <code>{@link #getChild(String)}</code>
     * and <code>{@link HstSiteMap#getSiteMapItem(String)}</code>
     * @return the value of this <code>SiteMapItem</code> which represents the logical path element for this <code>SiteMapItem</code>
     */
    String getValue();
    
    /**
     * Returns a boolean indicating whether this <code>HstSiteMapItem</code> represents a path with a <code>wildcard</code> value <code>*</code> 
     * @return <code>true</code> if this <code>HstSiteMapItem</code> represents <code>*</code> 
     */
    boolean isWildCard();
    
    /**
     * Returns a boolean indicating whether this <code>HstSiteMapItem</code> represents a path with a <code>any</code> value <code>**</code> 
     * @return <code>true</code> if this <code>HstSiteMapItem</code> represents <code>**</code> 
     */
    boolean isAny();
    
    /**
     * This method returns a content path, relative to the {@link org.hippoecm.hst.configuration.HstSite#getContentPath()}. This value can 
     * contain property placeholders, like ${1}/${2}, which should be resolved in the {@link org.hippoecm.hst.core.request.ResolvedSiteMapItem#getRelativeContentPath()}  
     * @return the content path relative to the {@link org.hippoecm.hst.configuration.HstSite#getContentPath()}
     */
    String getRelativeContentPath();
    
    /**
     * If a HstSiteMapItem can be used to resolve a url, it must have a componentConfigurationId referencing 
     * the component configuration the will handle the request processing. This component configuration can be
     * the root of a component configuration tree, see {@link HstComponentConfiguration}.
     * @return the componentConfigurationId for this <code>SiteMapItem</code> or <code>null</code>
     */
    String getComponentConfigurationId();
    
    /**
     * If a HstSiteMapItem can be used to resolve a url, it must have a portletComponentConfigurationId referencing 
     * the portlet component configuration the will handle the request processing. This component configuration can be
     * the root of a component configuration tree, see {@link HstComponentConfiguration}.
     * @return the portletComponentConfigurationId for this <code>SiteMapItem</code> or <code>null</code>
     */
    String getPortletComponentConfigurationId();
    
    /**
     * {@link HstComponent} instances can access <code>HstSiteMapItem</code> but should not be able to modify them, implementations
     * should return an unmodifiable List
     * @return
     */
    List<String> getRoles();  
    
    /**
     * Returns a <code>List</code> of all child <code>HstSiteMapItem</code>'s of this <code>HstSiteMapItem</code>.    
     * Implementations should return an unmodifiable list, for example {@link java.util.Collections$UnmodifiableList} to avoid 
     * client code changing configuration
     * @return
     */
    List<HstSiteMapItem> getChildren();
    
    /**
     * Return the child <code>HstSiteMapItem</code> that has the corresponding <code>value</code> ({@link HstSiteMapItem#getValue()} ) 
     * and <code>null</code> otherwise
     * @param value the value of the child <code>HstSiteMapItem</code> as it would be return by {@link HstSiteMapItem#getValue()} 
     * @return Returns the HstSiteMapItem object corresponding to the unique <code>value</code> and <code>null</code> if no <code>HstSiteMapItem</code>
     * exists with this <code>value</code> in this <code>HstSiteMapItem</code> object. 
     */
    HstSiteMapItem getChild(String value);
    
    /**
     * A HstSiteMapItem can contain a Map of parameters. These parameters can be accessed from {@link HstComponent}'s instances through 
     * a parameter in the {@link HstComponentConfiguration}. For example, if this <code>SiteMapItem</code> would have a parameter named
     * <code>foo</code> and value <code>bar</code>, the {@link HstComponentConfiguration} linked through the 
     * {@link #getComponentConfigurationId()} can access this parameters by having an own parameter, 
     * for example named <code>lux</code> and the value <code>${foo}</code>. If the <code>HstSiteMapItem</code> is a WILDCARD or any of its
     * ancestors, you can also set the parameter values to <code>${1}</code>, <code>${2}</code> etc where <code>${1}</code> refers to the 
     * first matched wildcard, <code>${2}</code> to the second, etc.
     * @param name the name of the parameter
     * @return the value of the parameter
     */
    String getParameter(String name);
    
    /**
     * See {@link #getParameter(String)}, only now the parameters map is returned.
     * Implementations should return an unmodifiable map, for example {@link java.util.Collections$UnmodifiableMap} to avoid 
     * client code changing configuration
     * @return the Map of parameters contained in this <code>HstSiteMapItem</code>
     */
    Map<String, String> getParameters();
    
    /**
     * Returns parent <code>HstSiteMapItem</code> and <code>null</code> when the item does not have a parent (in other words, it is a
     * root <code>HstSiteMapItem</code>)
     * @return the parent <code>HstSiteMapItem</code> 
     */
    HstSiteMapItem getParentItem();
    
    /**
     * Return the <code>HstSiteMap</code> that is the container of this <code>HstSiteMapItem</code>
     * @return the <code>HstSiteMap</code> that is the container of this <code>HstSiteMapItem</code>
     */
    HstSiteMap getHstSiteMap();
    
    /**
     * A <code>HstSiteMapItem</code> can specify the status-code that needs to be set on the webpage header. If a status-code
     * is specified, it can be retrieved through this method. <code>0</code> is return is non is specified. 
     * @return if a status code is set, return this, otherwise <code>0</code>
     */
    int getStatusCode();
    
    /**
     * A <code>HstSiteMapItem</code> can specify an error-code. If an error-code
     * is specified, the framework will invoke a sendError(int code) where the code is the value returned
     * by this method . <code>0</code> is return is non is specified. 
     * @return if an error code is set, return this, otherwise <code>0</code>
     */
    int getErrorCode();
    
    /**
     * @return true if this SiteMapItem functions as a error sitemap item
     */
    boolean isErrorSiteMapItem();
}
