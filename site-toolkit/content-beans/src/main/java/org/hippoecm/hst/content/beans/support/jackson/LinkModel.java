/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.support.jackson;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;
import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_PIPELINE_NAME;

public class LinkModel {

    private static Logger log = LoggerFactory.getLogger(LinkModel.class);

    private final String href;

    /**
     * The type of the link, for example 'internal', 'external' or 'resource'.
     */
    private final String type;

    private final String rel;

    private final String title;

    public LinkModel(final String href) {
        this(href, null, null, null);
    }

    public LinkModel(final String href, final String type) {
        this(href, type, null, null);
    }

    public LinkModel(final String href, final String type, final String rel, final String title) {
        this.href = href;
        this.type = type;
        this.rel = rel;
        this.title = title;
    }

    @JsonInclude(Include.NON_NULL)
    public String getHref() {
        return href;
    }

    @JsonInclude(Include.NON_NULL)
    public String getType() {
        return type;
    }

    @JsonInclude(Include.NON_NULL)
    public String getRel() {
        return rel;
    }

    @JsonInclude(Include.NON_NULL)
    public String getTitle() {
        return title;
    }

    /**
     * @param hstLink
     * @return the {@code hstLink} converted to a {@link LinkModel}
     */
    public static LinkModel convert(final HstLink hstLink, final HstRequestContext requestContext) {
        final Mount linkMount = hstLink.getMount();
        // admittedly a bit of a dirty check to check on PageModelPipeline. Can this be improved?
        if (PAGE_MODEL_PIPELINE_NAME.equals(linkMount.getNamedPipeline())) {
            final Mount siteMount = linkMount.getParent();
            if (siteMount == null) {
                log.info("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                        "case for '{}'. Return the link model of the current hst link instead of relative to the parent " +
                        "mount.", linkMount);
                return new LinkModel(hstLink.toUrlForm(requestContext, false), "external");
            }
            // since the selfLink could be resolved, the site link also must be possible to resolve
            final HstLink siteLink = requestContext.getHstLinkCreator().create(hstLink.getPath(), siteMount);
            if (siteLink != null && siteLink.getHstSiteMapItem() != null) {
                final HstSiteMapItem siteMapItem = siteLink.getHstSiteMapItem();
                final String linkType;
                if (siteMapItem.isContainerResource()) {
                    linkType = "resource";
                } else {
                    final String linkApplicationId = siteMapItem.getApplicationId();
                    // although this is the resolved sitemap item for the PAGE_MODEL_PIPELINE_NAME, it should resolve
                    // to exactly the same hst sitemap item configuration node as the parent mount, hence we can compare
                    // the application id. If there is *no* application id set for both site map items, the link type is
                    // internal. *If* the SpaSitePipeline is configured on site map item level, a site map item *MUST*
                    // have an application id to have correct indication of 'internal/external'.
                    final String currentApplicationId = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getApplicationId();
                    linkType = Objects.equals(linkApplicationId, currentApplicationId) ? "internal" : "external";
                }
                return new LinkModel(siteLink.toUrlForm(requestContext, false), linkType);
            }

            log.warn("Unexpectedly could not resolve a site link for '{}'. Return LinkModel for hst link.", hstLink);

            return new LinkModel(hstLink.toUrlForm(requestContext, false), "external");

        }

        if (requestContext.getResolvedMount().getMount() == hstLink.getMount()) {
            return new LinkModel(hstLink.toUrlForm(requestContext, false), "internal");
        }

        return new LinkModel(hstLink.toUrlForm(requestContext, false), "external");
    }

}
