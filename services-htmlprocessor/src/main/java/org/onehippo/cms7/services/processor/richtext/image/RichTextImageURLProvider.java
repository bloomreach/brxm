/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.richtext.image;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.util.FacetUtil;
import org.onehippo.cms7.services.processor.richtext.UrlProvider;
import org.onehippo.cms7.services.processor.richtext.RichTextException;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextImageURLProvider implements UrlProvider {
    public static final Logger log = LoggerFactory.getLogger(RichTextImageURLProvider.class);

    private static final String IMAGE_NOT_FOUND = "data:image/gif;base64,R0lGODlhEAAQAMQAAORHHOVSKudfOulrSOp3WOyDZu6QdvCchPGolfO0o/XBs/fNwfjZ0frl3/zy7////wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAkAABAALAAAAAAQABAAAAVVICSOZGlCQAosJ6mu7fiyZeKqNKToQGDsM8hBADgUXoGAiqhSvp5QAnQKGIgUhwFUYLCVDFCrKUE1lBavAViFIDlTImbKC5Gm2hB0SlBCBMQiB0UjIQA7";

    private final RichTextImageFactory imageFactory;
    private final RichTextLinkFactory linkFactory;
    private final Model<Node> nodeModel;

    public RichTextImageURLProvider(final RichTextImageFactory factory, final RichTextLinkFactory linkFactory,
                                    final Model<Node> nodeModel) {
        this.imageFactory = factory;
        this.linkFactory = linkFactory;
        this.nodeModel = nodeModel;
    }

    @Override
    public String getURL(final String link) {
        try {
            final String facetName = StringUtils.substringBefore(link, "/");
            final String type = StringUtils.substringAfterLast(link, "/");
            final Node node = this.nodeModel.get();
            final String uuid = FacetUtil.getChildDocBaseOrNull(node, facetName);
            if (uuid != null && linkFactory.getLinkUuids().contains(uuid)) {
                final RichTextImage rti = imageFactory.loadImageItem(uuid, type);
                return rti.getUrl();
            }
            return link;
        } catch (final RichTextException e) {
            log.error("Error creating image link for input '{}'", link, e);
            return IMAGE_NOT_FOUND;
        }
    }
}
