/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container.site;

import java.util.Map;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.HstRequestContext;

@FunctionalInterface
public interface HstSiteProvider {

    /**
     * <p>
     *     An {@link HstSiteProvider} can choose based on some heuristic to return a specific {@link HstSite} branch
     *     or to return the master.
     * </p>
     * <p>
     *     Note that this method {@link HstSiteProvider#getHstSite(HstSite, Map, HstRequestContext)}
     *     only gets invoked when there is at least one branch for the {@link HstSite} belonging to the matched
     *     {@link org.hippoecm.hst.configuration.hosting.Mount} for the current {@link HstRequestContext}. If you provide
     *     a custom {@link HstSiteProvider} it might thus happen that your implemented
     *     {@link HstSiteProvider#getHstSite(HstSite, Map, HstRequestContext)} does not get invoked, which is most
     *     likely caused by there being no branches available.
     * </p>
     * @param master the master {@link HstSite}
     * @param branches unmutable map of {link HstSite}s which are the branches (master not included!) and empty map if no
     *                 branches for {@code master} present. They keys in the map are the branch ids
     * @param requestContext
     * @return the {@link HstSite} to use for rendering, not allowed to be {@code null}
     */
    HstSite getHstSite(HstSite master, Map<String, HstSite> branches, HstRequestContext requestContext);

}
