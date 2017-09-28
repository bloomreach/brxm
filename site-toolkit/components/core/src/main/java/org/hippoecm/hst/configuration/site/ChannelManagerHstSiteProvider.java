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

import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;

public class ChannelManagerHstSiteProvider implements HstSiteProvider {

    private static final Logger log = LoggerFactory.getLogger(ChannelManagerHstSiteProvider.class);

    @Override
    public HstSite getHstSite(final CompositeHstSite compositeHstSite, final HstRequestContext requestContext) {
        HttpSession session = requestContext.getServletRequest().getSession();

        final String renderingMountId = (String) session.getAttribute(CMS_REQUEST_RENDERING_MOUNT_ID);
        Mount mount;
        if (renderingMountId == null) {
            mount = requestContext.getResolvedMount().getMount();
        } else {
            mount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(renderingMountId);
        }
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
        if (cmsSessionContext == null) {
            log.debug("No branch selected for mount '{}'. Return master", mount);
            return compositeHstSite.getMaster();
        }
        final String branchId = (String) cmsSessionContext.getAttribute(ATTRIBUTE_ACTIVE_PROJECT_ID);
        if (branchId == null) {
            log.debug("No branch selected for mount '{}'. Return master", mount);
            return compositeHstSite.getMaster();
        }
        HstSite branch = compositeHstSite.getBranches().get(branchId);
        if (branch == null) {
            log.info("No branch found with branchId '{}' for mount '{}', probably site is requested for rendering a cross-site link or project has been deleted. ", branchId, mount);
            return compositeHstSite.getMaster();
        }
        log.info("Using branch '{}' for mount '{}'", branch.getName(), mount);
        return branch;
    }
}
