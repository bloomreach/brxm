/*
 *  Copyright 20014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllLinksForANewsItem extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(AllLinksForANewsItem.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        final HstRequestContext requestContext = request.getRequestContext();

        HippoBean newsBean = requestContext.getSiteContentBaseBean().getBean("news/2009/01/launch-cms-7");
        Node newsNode = newsBean.getNode();

        final HstLinkCreator hstLinkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());

        final List<HstLink> allLinks = new ArrayList<>();

        final List<HstLink> allAvailableCanonicals = hstLinkCreator.createAllAvailableCanonicals(newsNode, requestContext);

        // now per canonical link, add all extra possible links as well
        for (HstLink canonicalLink : allAvailableCanonicals) {
            augmentLinks(allLinks, canonicalLink, hstLinkCreator, newsNode);
        }

        // now we most likely have picked up some duplicate links. Filter duplicates out
        final List<HstLink> filteredAllLinks = filterDuplicates(allLinks, requestContext);

        request.setAttribute("hstLinks", filteredAllLinks);
    }

    private void augmentLinks(final List<HstLink> allLinks, final HstLink canonicalLink,
                              final HstLinkCreator hstLinkCreator, final Node newsNode) {
        allLinks.add(canonicalLink);

        final Mount mount = canonicalLink.getMount();
        final HstSiteMap siteMap = mount.getHstSite().getSiteMap();
        for (HstSiteMapItem item : siteMap.getSiteMapItems()) {
            traverseSiteMapItemAndAddHits(allLinks, item, hstLinkCreator, mount, newsNode);
        }
    }

    private void traverseSiteMapItemAndAddHits(final List<HstLink> allLinks,
                                               final HstSiteMapItem item,
                                               final HstLinkCreator hstLinkCreator,
                                               final Mount mount,
                                               final Node newsNode) {
        final HstLink hstLink = hstLinkCreator.create(newsNode, mount, item, false);
        if (!hstLink.isNotFound()) {
            // add to the allLinks list and try descendant sitemap items as well
            allLinks.add(hstLink);
            for (HstSiteMapItem child : item.getChildren()) {
                traverseSiteMapItemAndAddHits(allLinks, child, hstLinkCreator, mount, newsNode);
            }
        }
    }

    private List<HstLink> filterDuplicates(final List<HstLink> allLinks,
                                  final HstRequestContext requestContext) {

        HashMap<String, HstLink> uniqueLinks = new HashMap<>();
        for (HstLink link : allLinks) {
            // same links result in one entry
            uniqueLinks.put(link.toUrlForm(requestContext, false), link);
        }
        return new ArrayList<>(uniqueLinks.values());
    }


}