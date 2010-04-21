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

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;


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
     * correct {@link VirtualHost}. If it does find a {@link VirtualHost}, the match is delegated to
     * {@link VirtualHost#match(HttpServletRequest)}</p>. If no {@link VirtualHost} matches the request, then, if configured, we try
     * to match to the default virtual host {@link #getDefaultHostName()}.
     * 
     * @see {@link VirtualHost#match(HttpServletRequest)} and  {@link SiteMount#match(HttpServletRequest)}
     *
     * @param request the HttpServletRequest
     * @return the resolvedSiteMapItem for this request or <code>null</code> when it can not be matched
     * @throws MatchException when the matching cannot be done, for example because no valid virtual hosts are configured
     */
    ResolvedSiteMapItem match(HttpServletRequest request) throws MatchException;
 
    /**
     * 
     * @return the hostname that is configured as default, or <code>null</code> if none is configured as default.
     */
    String getDefaultHostName();
}
