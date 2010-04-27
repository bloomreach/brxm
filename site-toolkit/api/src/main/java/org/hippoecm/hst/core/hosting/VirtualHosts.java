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
package org.hippoecm.hst.core.hosting;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;


/**
 * The container interface for {@link VirtualHost}
 * 
 */
public interface VirtualHosts {

    /**
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
     * <p>This method tries to match a request to a flyweight {@link ResolvedSiteMapItem}. It does so, by first trying to match the 
     * correct {@link ResolvedVirtualHost}. If it does find a {@link ResolvedVirtualHost}, the match is delegated to
     * {@link ResolvedVirtualHost#matchSiteMountItem(HttpServletRequest)}, which returns the {@link ResolvedSiteMount}. This object
     * delegates to {@link ResolvedSiteMount#matchSiteMapItem(HttpServletRequest)} which in the end returns the {@link ResolvedSiteMapItem}. If somewhere
     * in the chain a match cannot be made, <code>null</code> will be returned. 
     * </p>
     * 
     * @param request the HttpServletRequest
     * @return the resolvedSiteMapItem for this request or <code>null</code> when it can not be matched to a sitemap item
     * @throws MatchException when the matching cannot be done, for example because no valid virtual hosts are configured
     */
    ResolvedSiteMapItem matchSiteMapItem(HttpServletRequest request) throws MatchException;
    
    /**
     * <p>This method tries to match a request to a flyweight {@link ResolvedSiteMount}. It does so, by first trying to match the 
     * correct {@link ResolvedVirtualHost}. If it does find a {@link ResolvedVirtualHost}, the match is delegated to
     * {@link ResolvedVirtualHost#matchSiteMountItem(HttpServletRequest)}, which returns the {@link ResolvedSiteMount}. If somewhere
     * in the chain a match cannot be made, <code>null</code> will be returned. 
     * </p>
     * @param request the HttpServletRequest
     * @return the resolvedSiteMount for this request or <code>null</code> when it can not be matched to a siteMount
     * @throws MatchException
     */
    ResolvedSiteMount matchSiteMountItem(HttpServletRequest request) throws MatchException;
    
    /**
     * <p>
     *  This method tries to match a request to a flyweight {@link ResolvedVirtualHost}
     * </p>
     * @param request
     * @return the resolvedVirtualHost for this request or <code>null</code> when it can not be matched to a virtualHost
     * @throws MatchException
     */
    ResolvedVirtualHost matchVirtualHost(HttpServletRequest request) throws MatchException;
 
    /**
     * Returns the list of all available hosts managed by this VirtualHosts object. When <code>mountableOnly</code> is <code>false</code>, you might get
     * hosts back your are not interested in: For example, when the host, www.onehippo.org is added, then the cleanest way to configure this is:
     * <pre>
     * org
     *  ` onehippo
     *        ` www
     * </pre>
     * Now, quite likely, only the 'www' host segment has a {@link SiteMount} attached to it. However, this does not mean that by itself, 'org' and 'onehippo.org' are 
     * as well hosts. When <code>mountedOnly</code> is <code>false</code>, you get back three hosts. When however <code>mountedOnly</code> is <code>true</code>,
     * only those hosts that have a {@link SiteMount} attached <b>and</b> for which at least <code>1</code> SiteMount points to a correct mountPath, in other words, have 
     * an existing {@link HstSite} they point to  
     * @param mountedOnly when <code>true</code>, only the list of virtual hosts are returned that have a {@link SiteMount} that have a correct <code>mountPath</code>
     * @return the list of VirtualHosts, and an empty list when no hosts apply
     */
    List<VirtualHost> getVirtualHosts(boolean mountedOnly);
    
    /**
     * 
     * @return the hostname that is configured as default, or <code>null</code> if none is configured as default.
     */
    String getDefaultHostName();
}
