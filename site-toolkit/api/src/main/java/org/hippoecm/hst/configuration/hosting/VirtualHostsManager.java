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

import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;


public interface VirtualHostsManager {

    /**
     * @return the <code>VirtualHosts</code> managed by this VirtualHostsManager
     */
    VirtualHosts getVirtualHosts() throws RepositoryNotAvailableException;
    
    /**
     * VirtualHostsManager must contain a reference to the {@link HstSiteMapMatcher} that is being used. You can inject your own
     *  {@link HstSiteMapMatcher} implementation if needed
     * @return the global HstSiteMapMatcher implementation used for all the hosts & sites
     */
    HstSiteMapMatcher getHstSiteMapMatcher();
    
    /**
     * Invalidates this VirtualHostsManager. Typically this invalidate is called after a received event indicating that for example
     * the backing hosts configuration has been changed.
     */
    void invalidate(String path);
}
