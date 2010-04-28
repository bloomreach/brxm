/*
 *  Copyright 2010 Hippo.
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

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;

/**
 * Implementations of this interface are a request flyweight instance of the {@link VirtualHost} object, where possible wildcard property placeholders have been filled in, similar
 * to the {@link ResolvedSiteMount} and {@link SiteMount}
 */
public interface ResolvedVirtualHost {

    /**
     * @return the backing virtual host of this ResolvedVirtualHost
     */
    VirtualHost getVirtualHost();
    
    /**
     * the hostName that resolved to this ResolvedVirtualHost. Note that this might be different then the one in {@link VirtualHost#getHostName()} as this
     * hostName might contain not yet filled in wildcards. 
     * @see VirtualHost#getHostName()
     * @return the hostName that resolved to this ResolvedVirtualHost
     */
    String getResolvedHostName();

    /**
     * matches the current request and resolved virtual host to a {@link ResolvedSiteMount} item
     * @param request
     * @return a {@link ResolvedSiteMount} or <code>null</code> when none matches
     */
    ResolvedSiteMount matchSiteMountItem(HttpServletRequest request);
    
}
