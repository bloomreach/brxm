/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.richtext.link;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.service.FacetService;
import org.onehippo.cms7.services.htmlprocessor.util.LinkUtil;
import org.onehippo.cms7.services.htmlprocessor.visit.FacetTagProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextLinkTagProcessor implements FacetTagProcessor {
    public static final Logger log = LoggerFactory.getLogger(RichTextLinkTagProcessor.class);

    public static final String TAG_A = "a";
    public static final String ATTRIBUTE_HREF = "href";

    @Override
    public void onRead(final Tag tag, final FacetService facets) {
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_A, tag.getName())) {
            convertLinkForRetrieval(tag, facets);
        }
    }

    @Override
    public void onWrite(final Tag tag, final FacetService facetService) {
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_A, tag.getName())) {
            convertLinkForStorage(tag, facetService);
        }
    }

    private void convertLinkForRetrieval(final Tag tag, final FacetService facetService) {
        final String href = tag.getAttribute(ATTRIBUTE_HREF);

        if (StringUtils.isEmpty(href)) {
            return;
        }
        if (LinkUtil.isExternalLink(href)) {
            return;
        }

        final String path = StringUtils.substringBefore(href, "#");
        final String fragmentId = StringUtils.substringAfter(href, "#");
        final String name = NodeNameCodec.encode(path, true);
        final String uuid = facetService.getFacetId(name);

        if (uuid != null) {
            // set href to 'http://' so CKEditor handles it as a link instead of an anchor
            tag.addAttribute(ATTRIBUTE_HREF, LinkUtil.INTERNAL_LINK_DEFAULT_HREF);
            tag.addAttribute(ATTRIBUTE_DATA_UUID, uuid);
            tag.addAttribute(ATTRIBUTE_DATA_FRAGMENT_ID, fragmentId);

            facetService.markVisited(name);
        }
    }

    private void convertLinkForStorage(final Tag tag, final FacetService facetService) {
        final String href = tag.getAttribute(ATTRIBUTE_HREF);

        if (StringUtils.isEmpty(href)) {
            return;
        }

        final String fragmentId = tag.getAttribute(ATTRIBUTE_DATA_FRAGMENT_ID);
        tag.removeAttribute(ATTRIBUTE_DATA_FRAGMENT_ID);

        final String uuid = tag.getAttribute(ATTRIBUTE_DATA_UUID);
        tag.removeAttribute(ATTRIBUTE_DATA_UUID);

        if (LinkUtil.isExternalLink(href)) {
            return;
        }

        if (uuid == null) {
            return;
        }

        String name = facetService.findOrCreateFacet(uuid);
        if (name == null) {
            return;
        }

        final String newHref = StringUtils.isEmpty(fragmentId) ? name : name + "#" + fragmentId;
        tag.addAttribute(ATTRIBUTE_HREF, newHref);
        facetService.markVisited(name);
    }
}
