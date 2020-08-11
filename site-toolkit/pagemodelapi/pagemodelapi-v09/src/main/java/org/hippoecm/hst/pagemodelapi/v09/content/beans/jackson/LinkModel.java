/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson;

import java.util.Objects;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_PIPELINE_NAME;
import static org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.LinkModel.LinkType.EXTERNAL;
import static org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.LinkModel.LinkType.INTERNAL;
import static org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.LinkModel.LinkType.RESOURCE;
import static org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.LinkModel.LinkType.UNKNOWN;

@ApiModel(description = "Link model.")
public class LinkModel {

    public enum LinkType {

        RESOURCE("resource"),
        EXTERNAL("external"),
        INTERNAL("internal"),
        UNKNOWN("unknown");

        private final String type;

        LinkType(final String type) {

            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    private static Logger log = LoggerFactory.getLogger(LinkModel.class);

    private final String href;

    /**
     * The type of the link, for example 'internal', 'external' or 'resource'.
     */
    private final LinkType type;

    private final String rel;

    private final String title;

    public LinkModel(final String href) {
        this(href, null, null, null);
    }

    public LinkModel(final String href, final LinkType type) {
        this(href, type, null, null);
    }

    public LinkModel(final String href, final LinkType type, final String rel, final String title) {
        this.href = href;
        this.type = type;
        this.rel = rel;
        this.title = title;
    }

    @JsonInclude(Include.NON_NULL)
    @ApiModelProperty("Location of the link.")
    public String getHref() {
        return href;
    }

    @JsonInclude(Include.NON_NULL)
    @ApiModelProperty("Type of the link.")
    public String getType() {
        if (type == null) {
            return null;
        }
        return type.getType();
    }

    @JsonInclude(Include.NON_NULL)
    @ApiModelProperty("Relationship name of the link.")
    public String getRel() {
        return rel;
    }

    @JsonInclude(Include.NON_NULL)
    @ApiModelProperty("Title of the link.")
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
        HstLink siteLink;

        if (PAGE_MODEL_PIPELINE_NAME.equals(linkMount.getNamedPipeline())) {
            siteLink = getSiteLink(hstLink, requestContext);
            if (siteLink == null) {
                log.warn("Could not get a site link. Use hstLink from argument instead instead.");
                siteLink = hstLink;
            }
        } else {
            siteLink = hstLink;
        }

        final LinkType linkType = getLinkType(requestContext, siteLink);
        final String href;
        if (linkType == UNKNOWN) {
            href = null;
        } else if (linkType == INTERNAL) {
            href = siteLink.toUrlForm(requestContext, false);
        } else {
            // 'resource' URLs (eg binaries) and 'external' types for Page Model API always must be fully qualified
            href = siteLink.toUrlForm(requestContext, true);
        }
        return new LinkModel(href, linkType);

    }


    public static LinkType getLinkType(final HstRequestContext requestContext, final HstLink siteLink) {
        if (siteLink.isNotFound()) {
            return UNKNOWN;
        }

        if (siteLink.isContainerResource()) {
            return RESOURCE;
        }

        final Mount linkMount = siteLink.getMount();
        if (linkMount != null && linkMount != requestContext.getResolvedMount().getMount().getParent()) {
            // this is a cross mount link since does not belong to the parent mount of the PageModelApi mount
            return EXTERNAL;
        }

        final HstSiteMapItem siteMapItem = siteLink.getHstSiteMapItem();
        if (siteMapItem == null) {
            return EXTERNAL;
        }
        final String linkApplicationId = siteMapItem.getApplicationId();
        // although this is the resolved sitemap item for the PAGE_MODEL_PIPELINE_NAME, it should resolve
        // to exactly the same hst sitemap item configuration node as the parent mount, hence we can compare
        // the application id. If there is *no* application id set for both site map items, the link type is
        // internal. *If* the SpaSitePipeline is configured on site map item level, a site map item *MUST*
        // have an application id to have correct indication of 'internal/external'.
        final String currentApplicationId = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getApplicationId();
        return Objects.equals(linkApplicationId, currentApplicationId) ? INTERNAL : EXTERNAL;
    }

    private static HstLink getSiteLink(final HstLink hstLink, final HstRequestContext requestContext) {
        final Mount linkMount = hstLink.getMount();
        final Mount siteMount = linkMount.getParent();
        if (siteMount == null) {
            log.info("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                    "case for '{}'. Return null.", linkMount);
            return null;
        }
        // since the selfLink could be resolved, the site link also must be possible to resolve
        final HstLink siteLink = requestContext.getHstLinkCreator().create(hstLink.getPath(), siteMount);
        if (siteLink != null) {
            if (hstLink.isNotFound()) {
                siteLink.setNotFound(true);
            }
            return siteLink;
        }

        log.warn("Unexpectedly could not resolve a site link for '{}'. Return null.", hstLink);

        return null;
    }

}
