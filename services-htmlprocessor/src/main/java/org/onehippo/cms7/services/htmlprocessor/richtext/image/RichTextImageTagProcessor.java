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
package org.onehippo.cms7.services.htmlprocessor.richtext.image;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLProvider;
import org.onehippo.cms7.services.htmlprocessor.service.FacetService;
import org.onehippo.cms7.services.htmlprocessor.util.LinkUtil;
import org.onehippo.cms7.services.htmlprocessor.visit.FacetTagProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImage.DOCUMENT_PATH_PLACEHOLDER;
import static org.onehippo.cms7.services.htmlprocessor.util.JcrUtil.PATH_SEPARATOR;
import static org.onehippo.cms7.services.htmlprocessor.visit.FacetVisitor.ATTRIBUTE_DATA_UUID;

public class RichTextImageTagProcessor implements FacetTagProcessor {

    public static final Logger log = LoggerFactory.getLogger(RichTextImageTagProcessor.class);

    public static final String TAG_IMG = "img";
    public static final String ATTRIBUTE_SRC = "src";
    public static final String ATTRIBUTE_DATA_TYPE = "data-type";

    private final URLProvider imageURLProvider;

    public RichTextImageTagProcessor(final URLProvider imageURLProvider) {
        this.imageURLProvider = imageURLProvider;
    }

    @Override
    public void onRead(final Tag tag, final FacetService facets) {
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_IMG, tag.getName())) {
            convertImageForRetrieval(tag, facets);
        }
    }

    @Override
    public void onWrite(final Tag tag, final FacetService facetService) {
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_IMG, tag.getName())) {
            try {
                convertImageForStorage(tag, facetService);
            } catch (final RepositoryException e) {
                log.warn("Failed to process image tag on write, src={}, data-uuid={}",
                         tag.getAttribute(ATTRIBUTE_SRC), tag.getAttribute(ATTRIBUTE_DATA_UUID), e);
            }
        }
    }

    private void convertImageForRetrieval(final Tag tag, final FacetService facetService) {
        final String src = tag.getAttribute(ATTRIBUTE_SRC);

        if (StringUtils.isEmpty(src) || LinkUtil.isExternalLink(src)) {
            return;
        }

        final String[] parts = src.split(PATH_SEPARATOR);
        final String name = parts.length >= 1 ? parts[0] : null;
        final String uuid = facetService.getFacetId(name);

        if (uuid != null) {
            tag.addAttribute(ATTRIBUTE_SRC, imageURLProvider.getURL(src));
            tag.addAttribute(ATTRIBUTE_DATA_UUID, uuid);

            final String type = parts.length >= 3 ? parts[2] : null;
            if (type != null) {
                tag.addAttribute(ATTRIBUTE_DATA_TYPE, type);
            }

            facetService.markVisited(name);
        }
    }

    private void convertImageForStorage(final Tag tag, final FacetService facetService) throws RepositoryException {
        if (!tag.hasAttribute(ATTRIBUTE_SRC) || StringUtils.isEmpty(tag.getAttribute(ATTRIBUTE_SRC))) {
            return;
        }

        final String uuid = tag.getAttribute(ATTRIBUTE_DATA_UUID);
        tag.removeAttribute(ATTRIBUTE_DATA_UUID);
        final String type = tag.getAttribute(ATTRIBUTE_DATA_TYPE);
        tag.removeAttribute(ATTRIBUTE_DATA_TYPE);

        if (uuid == null) {
            return;
        }

        final String name = facetService.findOrCreateFacet(uuid);
        if (name == null) {
            return;
        }

        String src = name;
        if (type != null) {
            src += PATH_SEPARATOR + DOCUMENT_PATH_PLACEHOLDER + PATH_SEPARATOR + type;
        }
        tag.addAttribute(ATTRIBUTE_SRC, src);

        facetService.markVisited(name);
    }
}
