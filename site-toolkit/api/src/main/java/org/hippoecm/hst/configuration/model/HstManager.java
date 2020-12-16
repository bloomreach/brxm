/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;

public interface HstManager {

    /**
     * @return the <code>VirtualHosts</code>, aka the HST model.
     */
    VirtualHosts getVirtualHosts() throws ContainerException;


    /**
     * a HstSitesManager must contain a reference to the {@link HstSiteMapMatcher} that is being used. You can inject your own
     *  {@link HstSiteMapMatcher} implementation if needed
     * @return the global HstSiteMapMatcher implementation used for all the hosts & sites
     */
    HstSiteMapMatcher getSiteMapMatcher();

    /**
     *
     * Some paths should not be handled by the hst framework request processing, eg /ping/
     *
     * When a path must be excluded, this method return true.
     *
     * @param pathInfo
     * @return true when the path must be excluded for matching to a host.
     * @deprecated since 13.0.0 : use {@link VirtualHosts#isHstFilterExcludedPath(String)}
     */
    @Deprecated
    boolean isHstFilterExcludedPath(String pathInfo);

    /**
     * Returns the request path suffix delimiter.
c     */
    String getPathSuffixDelimiter();

    String[] getHstFilterPrefixExclusions();

    String[] getHstFilterSuffixExclusions();


    /**
     * @return the contextPath of the current webapp
     */
    String getContextPath();


}