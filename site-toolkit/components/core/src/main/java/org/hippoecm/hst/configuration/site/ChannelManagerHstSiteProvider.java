/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerHstSiteProvider implements HstSiteProvider {

    private static final Logger log = LoggerFactory.getLogger(ChannelManagerHstSiteProvider.class);

    @Override
    public HstSite getHstSite(final CompositeHstSite compositeHstSite, final HstRequestContext requestContext) {
        Map<String, String> mountToBranchIdMapping =
                (Map<String, String>)requestContext.getServletRequest().getSession().getAttribute(HST_SITE_PROVIDER_HTTP_SESSION_KEY);
        Mount mount = requestContext.getResolvedMount().getMount();
        if (mountToBranchIdMapping == null) {
            log.debug("No branch selected for mount '{}'. Return master", mount);
            return compositeHstSite.getMaster();
        }
        final String branchId = mountToBranchIdMapping.get(mount.getIdentifier());
        if (branchId == null) {
            log.debug("No branch selected for mount '{}'. Return master", mount);
            return compositeHstSite.getMaster();
        }
        HstSite branch = compositeHstSite.getBranches().get(branchId);
        if (branch == null) {
            log.info("Unexpected branchId '{}' for mount '{}' found on http session because no such branch present. Removing " +
                    "branch now and return master.", branchId, mount);
            mountToBranchIdMapping.remove(branchId);
            return compositeHstSite.getMaster();
        }
        log.info("Using branch '{}' for mount '{}'", branch.getName(), mount);
        return branch;
    }
}
