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

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;

/**
 * Implementations of this interface are a request flyweight instance of the {@link VirtualHost} object, where possible wildcard property placeholders have been filled in, similar
 * to the {@link ResolvedMount} and {@link Mount}
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
     * @deprecated since HST 2.28.00 (CMS 7.9.0) use {@link #getVirtualHost()}#{@link org.hippoecm.hst.configuration.hosting.VirtualHost#getHostName()}
     * instead
     */
    @Deprecated
    String getResolvedHostName();

    /**
     * the portNumber that resolved to this ResolvedVirtualHost.
     * @return the portNumber that resolved to this ResolvedVirtualHost
     * @deprecated since HST 2.28.00 (CMS 7.9.0) use {@link org.hippoecm.hst.core.request.ResolvedMount#getPortNumber()}
     * instead
     */
    @Deprecated
    int getPortNumber();

    /**
     * matches the current requestPath and resolved virtual host to a {@link ResolvedMount} item. When the <code>contextPath</code> param is not <code>null</code> it 
     * might influence the matching, depending whether {@link Mount#onlyForContextPath()} is not <code>null</code> 
     * @param contextPath the contextPath if needed for matching. This parameter is allowed to be <code>null</code>
     * @param requestPath
     * @return a {@link ResolvedMount} or <code>null</code> when none matches
     */
    ResolvedMount matchMount(String contextPath, String requestPath)  throws MatchException;
    
}
