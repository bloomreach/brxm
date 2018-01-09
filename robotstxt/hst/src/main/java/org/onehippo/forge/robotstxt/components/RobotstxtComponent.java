/*
 * Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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

import com.google.common.base.Strings;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.onehippo.forge.robotstxt.annotated.Robotstxt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.List;

public class RobotstxtComponent extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(RobotstxtComponent.class);

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
        final Robotstxt bean = request.getRequestContext().getContentBean(Robotstxt.class);
        if (bean == null) {
            throw new HstComponentException("No bean found, check the HST configuration for the RobotstxtComponent");
        }
        request.setAttribute("document", bean);

        // Handle faceted navigation
        if (isFacetedNavigationDisallowed(request, (Robotstxt) bean)) {
            request.setAttribute("disallowedFacNavLinks", getDisallowedFacetNavigationLinks(request, mount));
        }
    }

    /**
     * Handle faceted navigation URLs. Typically, they should be disallowed, but a flag on the
     * robots.txt document can override this behavior and allow faceted navigation URLs.
     *
     * @param request Handle for current request
     * @param bean    The robots.txt document (CMS content)
     */
    protected boolean isFacetedNavigationDisallowed(final HstRequest request, final Robotstxt bean) {
        return bean.isDisallowFacNav();
    }

    /**
     * Disallow faceted navigation URLs.
     *
     * @param request      Handle for current request
     * @param currentMount Mount point of current site
     * @return List of HstLinks, representing Facet Navigation URLs.
     */
    protected List<HstLink> getDisallowedFacetNavigationLinks(final HstRequest request, final Mount currentMount) {

        final List<HstLink> disallowedLinks = new ArrayList<>();

        final HstLinkCreator linkCreator = request.getRequestContext().getHstLinkCreator();
        final List<Mount> allMounts = getMountWithSubMounts(currentMount);

        try {
            // We're running a JCR query because HST queries don't find facetnavigation nodes.
            // The query gets all facetnavigation content nodes and matches later to be under the current mount's
            // content path (or under any submount paths)
            final QueryManager queryManager = request.getRequestContext().getSession().getWorkspace().getQueryManager();
            @SuppressWarnings("deprecation") final Query query = queryManager.createQuery(getFacNavQueryXPath(), Query.XPATH);
            query.setLimit(getFacNavQueryLimit());

            final NodeIterator nodeIterator = query.execute().getNodes();
            while (nodeIterator.hasNext()) {
                final Node facNavNode = nodeIterator.nextNode();

                final HstLink facNavLink = linkCreator.create(facNavNode, request.getRequestContext());
                if (facNavLink.isNotFound()) {
                    log.debug("Not disallowing link for facet nav node {}: its HST link is not-found link '{}'",
                            facNavNode.getPath(), facNavLink.toUrlForm(request.getRequestContext(), false));
                    continue;
                }

                final Mount facNavLinkMount = facNavLink.getMount();

                if (allMounts.stream().noneMatch(mount -> mount.equals(facNavLinkMount))) {
                    log.debug("Not disallowing links for facet navigation node {} because its link mount {} is not part " +
                                    "of the current mount (or one of its submounts) for paths {}", facNavNode.getPath(),
                            facNavLink.getMount().getContentPath(), allMounts.stream().map(mount -> mount.getContentPath()).toArray());
                    continue;
                }

                // disallow all first level links below facet navigation
                final NodeIterator facetChildNodesIterator = facNavNode.getNodes();
                while (facetChildNodesIterator.hasNext()) {
                    final Node facetChildNode = facetChildNodesIterator.nextNode();

                    if (!facetChildNode.isNodeType(FacNavNodeType.NT_FACETSAVAILABLENAVIGATION)) {
                        log.debug("Not disallowing link for facet child node {}: it is not of type {} but of type {}",
                                facetChildNode.getPath(), FacNavNodeType.NT_FACETSAVAILABLENAVIGATION, facetChildNode.getPrimaryNodeType().getName());
                        continue;
                    }

                    final HstLink link = linkCreator.create(facetChildNode, request.getRequestContext());

                    if (link.isNotFound()) {
                        log.debug("Not disallowing link for facet child node {}: its HST link is not-found link '{}'",
                                facetChildNode.getPath(), link.toUrlForm(request.getRequestContext(), false));
                        continue;
                    }

                    // Link is suitable for rendering.
                    disallowedLinks.add(link);
                }
            }
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
        return disallowedLinks;
    }

    /**
     * Get the XPath query for searching facet navigation nodes, that are to be checked for exclusion.
     */
    protected String getFacNavQueryXPath() {
        return "//element(*, " + FacNavNodeType.NT_FACETNAVIGATION + ")";
    }

    /**
     * Get the limit used in the query for facet navigation nodes, that are to be checked for exclusion.
     */
    protected long getFacNavQueryLimit() {
        long limit = 200;
        final String limitStr = this.getComponentParameter("facnavQueryLimit");
        if (!Strings.isNullOrEmpty(limitStr)) {
            try {
                limit = Long.valueOf(limitStr);
            } catch (NumberFormatException nfe) {
                log.warn("Value {} for parameter facnavQueryLimit is not a long, falling back to limit {}", limitStr, limit);
            }
        }
        return limit;
    }

    /**
     * Get a list of the mount content base path, plus all content base paths of its child mounts.
     */
    protected List<Mount> getMountWithSubMounts(Mount mount) {

        final List<Mount> allMounts = new ArrayList<>();

        allMounts.add(mount);

        // add only live submounts recursively (no preview or composer (_rp) types)
        for (Mount subMount : mount.getChildMounts()) {
            if (subMount.isOfType(Mount.LIVE_NAME)) {
                allMounts.addAll(getMountWithSubMounts(subMount));
            }
        }

        return allMounts;
    }

    /**
     * Overridable logic to disallow all URLs for preview sites (setting the "disallowAll" flag)
     *
     * @param request Handle for current request
     * @param mount   Mount point of current site
     * @return true if mount is of type "preview"
     */
    protected boolean disallowPreviewMount(final HstRequest request, final Mount mount) {
        return mount.isOfType(Mount.PREVIEW_NAME);
    }
}
