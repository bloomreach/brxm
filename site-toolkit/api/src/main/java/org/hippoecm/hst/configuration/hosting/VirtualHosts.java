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
package org.hippoecm.hst.configuration.hosting;

import java.util.List;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;


/**
 * The container interface for {@link VirtualHost}
 * 
 */
public interface VirtualHosts {

    /**
     * @return the {@link HstManager} for this VirtualHosts object
     */
    HstManager getHstManager();
    
    /**
     * 
     * Typically, some paths we do not want to be handle by the hst framework request processing. Typically, this would
     * be for example paths starting with /binaries/, or paths ending with some extension, like .pdf
     * 
     * When a path must be excluded, this method return true.
     * 
     * @param pathInfo
     * @return true when the path must be excluded for matching to a host. 
     */
    boolean isExcluded(String pathInfo);
    
    /**
     * <p>This method tries to match a hstContainerURL to a flyweight {@link ResolvedSiteMapItem}. It does so, by first trying to match the 
     * correct {@link ResolvedVirtualHost}. If it does find a {@link ResolvedVirtualHost}, the match is delegated to
     * {@link ResolvedVirtualHost#matchSiteMount(HstContainerURL)}, which returns the {@link ResolvedSiteMount}. This object
     * delegates to {@link ResolvedSiteMount#matchSiteMapItem(String)} which in the end returns the {@link ResolvedSiteMapItem}. If somewhere
     * in the chain a match cannot be made a MatchException exception is thrown
     * </p>
     * 
     * @param request the HttpServletRequest
     * @return the resolvedSiteMapItem for this request
     * @throws MatchException when the matching cannot be done, for example because no valid virtual hosts are configured or when the request path does not match 
     * a sitemap item
     */
    ResolvedSiteMapItem matchSiteMapItem(HstContainerURL hstContainerURL) throws MatchException;

    
    /**
     * <p>This method tries to match a hostName, contextPath and requestPath to a flyweight {@link ResolvedSiteMount}. It does so, by first trying to match the 
     * correct {@link ResolvedVirtualHost}. If it does find a {@link ResolvedVirtualHost}, the match is delegated to
     * {@link ResolvedVirtualHost#matchSiteMount(String, String)}, which returns the {@link ResolvedSiteMount}. If somewhere
     * in the chain a match cannot be made, <code>null</code> will be returned. The contextPath will only be of influence in the matching
     * when the SiteMount has a non-empty value for {@link SiteMount#onlyForContextPath()}. If {@link SiteMount#onlyForContextPath()} is <code>null</code> or empty,
     * the <code>contextPath</code> is ignored for matching.
     * </p>
     * @param String hostName
     * @param String contextPath the contextPath of the request
     * @param String requestPath
     * @return the resolvedSiteMount for this hstContainerUrl or <code>null</code> when it can not be matched to a siteMount
     * @throws MatchException
     */
    ResolvedSiteMount matchSiteMount(String hostName, String contextPath,  String requestPath) throws MatchException;
    
    /**
     * <p>
     *  This method tries to match a request to a flyweight {@link ResolvedVirtualHost}
     * </p>
     * @param hostName 
     * @return the resolvedVirtualHost for this hostName or <code>null</code> when it can not be matched to a virtualHost
     * @throws MatchException
     */
    ResolvedVirtualHost matchVirtualHost(String hostName) throws MatchException;
 
    /**
     * @return the hostname that is configured as default, or <code>null</code> if none is configured as default.
     */
    String getDefaultHostName();
    
    /**
     * @return the locale of this VirtualHosts object or <code>null</code> if no locale is configured
     */
    String getLocale();
    
    /**
     * Returns the {@link SiteMount} for this <code>hostGroupName</code>, <code>alias<code> and <code>type<code> having {@link SiteMount#getType()} equal to <code>type</code>. Returns <code>null</code> when no match
     * 
     * @param hostGroupName
     * @param alias the alias the site mount must have
     * @param type  the type (for example preview, live, composer) the siteMount must have. 
     * @return the {@link SiteMount} for this <code>hostGroupName</code>, <code>alias<code> and <code>type<code> having {@link SiteMount#getType()} equal to <code>type</code>. Returns <code>null</code> when no match
     */
    SiteMount getSiteMountByGroupAliasAndType(String hostGroupName, String alias, String type);
    
    /**
     * @param hostGroupName
     * @return the List<{@link SiteMount}> belonging to <code>hostGroupName</code> or <code>null</code> when there are no SiteMount's for <code>hostGroupName</code>
     */
    List<SiteMount> getSiteMountsByHostGroup(String hostGroupName);
}
