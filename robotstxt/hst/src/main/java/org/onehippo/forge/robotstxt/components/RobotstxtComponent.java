/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.robotstxt.components;

import java.util.List;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.onehippo.forge.robotstxt.annotated.Robotstxt;

public class RobotstxtComponent extends BaseHstComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);

        // Disallow everything for preview sites ?
        final Mount mount = request.getRequestContext().getResolvedMount().getMount();
        if (disallowPreviewMount(request, mount)) {
            request.setAttribute("disallowAll", true);
            return;
        }

        // Process the CMS-based robots.txt configuration
        HippoBean bean = request.getRequestContext().getContentBean();
        if (bean == null) {
            throw new HstComponentException("No bean found, check the HST configuration for the RobotstxtComponent");
        } else if (!(bean instanceof Robotstxt)) {
            throw new HstComponentException("Expected HippoBean of type Robotstxt but got " + bean.getClass().getName());
        }
        request.setAttribute("document", bean);

        // Handle faceted navigation
        if (isFacetedNavigationDisallowed(request, (Robotstxt)bean)) {
            request.setAttribute("disallowedFacNavLinks", getDisallowedFacetNavigationLinks(request, mount));
        }
    }

    /**
     * Handle faceted navigation URLs. Typically, they should be disallowed, but a flag on the
     * robots.txt document can override this behavior and allow faceted navigation URLs.
     * @param request Handle for current request
     * @param bean    The robots.txt document (CMS content)
     */
    protected boolean isFacetedNavigationDisallowed(final HstRequest request, final Robotstxt bean) {
        return bean.isDisallowFacNav();
    }

    /**
     * Disallow faceted navigation URLs.
     * @param request Handle for current request
     * @param mount   Mount point of current site
     * @return        List of HstLinks, representing Facet Navigation URLs.
     */
    protected List<HstLink> getDisallowedFacetNavigationLinks(final HstRequest request, final Mount mount) {
        List<HstLink> disallowedLinks = new ArrayList<HstLink>();

        try {
            /**
             * We're running a JCR query because HST queries don't find facetnavigation nodes.
             * The query includes all content of the current site (everything under a certain
             * mount). It has been discussed whether nodes with the hst:authenticated flag
             * raised should be excluded, but the idea is that facetnavigation nodes would
             * typically not have this flag raised.
             */

            final String siteContentBase = mount.getContentPath();
            final String xpath = "/jcr:root"
                    + ISO9075.encodePath(siteContentBase)
                    + "//element(*,hippofacnav:facetnavigation)";

            final QueryManager queryManager = request.getRequestContext().getSession().getWorkspace().getQueryManager();
            final NodeIterator nodeIterator = queryManager.createQuery(xpath, "xpath").execute().getNodes();
            final HstLinkCreator linkCreator = request.getRequestContext().getHstLinkCreator();

            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                HstLink link = linkCreator.create(node, request.getRequestContext());

                if (link.isNotFound()) {
                    continue;
                }

                if (link.getMount() != mount) {
                    /**
                     * Some projects combine the content of multiple sites into the same content tree.
                     * It may then happen that certain (faceted) content is only available on another
                     * site than the one for which robots.txt has been requested. In order to avoid that
                     * robots.txt ends up with links to different sites, we exclude such content here.
                     */
                    continue;
                }

                // Link is suitable for rendering.
                disallowedLinks.add(link);
            }
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
        return disallowedLinks;
    }

    /**
     * Overridable logic to disallow all URLs for preview sites (setting the "disallowAll" flag)
     *
     * @param request Handle for current request
     * @param mount   Mount point of current site
     * @return        true if mount is of type "preview"
     */
    protected boolean disallowPreviewMount(final HstRequest request, final Mount mount) {
        return "preview".equals(mount.getType());
    }
}
