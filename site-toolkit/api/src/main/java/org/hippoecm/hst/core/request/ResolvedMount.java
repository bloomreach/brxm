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
package org.hippoecm.hst.core.request;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * Implementations of this interface are a request flyweight instance of the {@link Mount} object, where possible wildcard property placeholders have been filled in, similar
 * to the {@link ResolvedSiteMapItem} and {@link HstSiteMapItem}
 */
public interface ResolvedMount {

    /**
     * @return the {@link ResolvedVirtualHost} for this {@link ResolvedMount}
     * @deprecate since 2.28.00 (CMS 7.9). Use {@link #getMount()) and then {@link org.hippoecm.hst.configuration.hosting.Mount#getVirtualHost()}
     * to get the the virtualhost. This method is deprecated because in case of a preview decorated mount, via the
     * {@link ResolvedVirtualHost} you get to the undecorated hosts and mounts
     */
    @Deprecated
    ResolvedVirtualHost getResolvedVirtualHost();

    /**
     * @return the port number of the host that was used during matching to this {@link ResolvedMount}
     */
    int getPortNumber();
    
    /**
     * @return the backing request independent {@link Mount} item for this {@link ResolvedMount} instance
     */
    Mount getMount();
    
    /**
     * @return the named pipeline to be used for this {@link Mount} or <code>null</code> when the default pipeline is to be used
     */
    String getNamedPipeline();
    
    /**
     * Returns the mountPath from the backing {@link Mount} where possible wildcard values might have been replaced. A <code>root</code> {@link Mount} returns an empty {@link String} (""). When the mountPath is non-empty, it always starts with a  <code>"/"</code>.
     * @see Mount#getMountPath()
     * @return the resolved mountPath for this {@link ResolvedMount}
     */
    String getResolvedMountPath();
    
    /**
     * Expert: In most circumstance, this {@link #getMatchingIgnoredPrefix()} will return <code>null</code>. Only when there
     * was a pathInfo prefix after the {@link HttpServletRequest#getContextPath()} that should be ignored during matching the request to 
     * a {@link ResolvedMount}, this method returns the ignored prefix. The returned String must have leading and trailing slashes all removed.
     * @return the prefix that was ignore during matching and <code>null</code> if there wasn't a ignored prefix
     */
    String getMatchingIgnoredPrefix();
    
    /**
     * matches a pathInfo to a {@link ResolvedSiteMapItem} item or throws a 
     * {@link MatchException} or {@link NotFoundException} when cannot resolve to a sitemap item
     * @param siteMapPathInfo
     * @return the ResolvedSiteMapItem for the current hstContainerURL 
     * @throws MatchException 
     */
    ResolvedSiteMapItem matchSiteMapItem(String siteMapPathInfo) throws MatchException;
    
    /**
     * If this method returns true, then only if the user is explicitly allowed or <code>servletRequest.isUserInRole(role)</code> returns <code>true</code> this
     * Mount is accessible for the request. 
     * 
     * If a Mount does not have a configuration for authenticated, the value from the parent item is taken.
     * 
     * @return <code>true</code> if the Mount is authenticated. 
     */
    boolean isAuthenticated();
    
    /**
     * Returns the roles that are allowed to access this Mount when {@link #isAuthenticated()} is true. If the Mount does not have any roles defined by itself, it
     * inherits them from the parent. If it defines roles, the roles from any ancestor are ignored. An empty set of roles
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     * 
     * @return The set of roles that are allowed to access this Mount. When no roles defined, the roles from the parent item are inherited. If none of the 
     * parent items have a role defined, an empty set is returned
     */
    Set<String> getRoles();  
    
    /**
     * Returns the users that are allowed to access this Mount when {@link #isAuthenticated()} is true. If the Mount does not have any users defined by itself, it
     * inherits them from the parent. If it defines users, the users from any ancestor are ignored. An empty set of users
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     * 
     * @return The set of users that are allowed to access this Mount. When no users defined, the users from the parent item are inherited. If none of the 
     * parent items have a user defined, an empty set is returned
     */
    Set<String> getUsers();
    
    /**
     * Returns true if subject based jcr session should be used for this Mount 
     * @return
     */
    boolean isSubjectBasedSession();
    
    /**
     * Returns true if subject based jcr session should be statefully managed. 
     * @return
     */
    boolean isSessionStateful();
    
    /**
     * Returns FORM Login Page
     * @return <code>true</code> if the Mount is authenticated. 
     */
    String getFormLoginPage();
    
}
