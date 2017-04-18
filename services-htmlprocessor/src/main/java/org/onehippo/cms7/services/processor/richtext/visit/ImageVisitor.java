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
package org.onehippo.cms7.services.processor.richtext.visit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.util.FacetUtil;
import org.onehippo.cms7.services.processor.html.util.LinkUtil;
import org.onehippo.cms7.services.processor.html.visit.FacetVisitor;
import org.onehippo.cms7.services.processor.html.visit.Tag;
import org.onehippo.cms7.services.processor.richtext.URLProvider;

import static org.onehippo.cms7.services.processor.html.util.JcrUtil.PATH_SEPARATOR;
import static org.onehippo.cms7.services.processor.richtext.image.RichTextImage.DOCUMENT_PATH_PLACEHOLDER;

public class ImageVisitor extends FacetVisitor {

    private static final String TAG_IMG = "img";
    private static final String ATTRIBUTE_SRC = "src";
    private static final String ATTRIBUTE_DATA_UUID = "data-uuid";
    private static final String ATTRIBUTE_DATA_TYPE = "data-type";

    private final URLProvider imageURLProvider;

    public ImageVisitor(final Model<Node> nodeModel, final URLProvider imageURLProvider) {
        super(nodeModel);
        this.imageURLProvider = imageURLProvider;
    }

    @Override
    public void onRead(final Tag parent, final Tag tag) throws RepositoryException {
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_IMG, tag.getName())) {
            convertImageForRetrieval(tag);
        }
    }

    @Override
    public void onWrite(final Tag parent, final Tag tag) throws RepositoryException {
        super.onWrite(parent, tag);
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_IMG, tag.getName())) {
            convertImageForStorage(tag);
        }
    }

    private void convertImageForRetrieval(final Tag tag) {
        final String src = tag.getAttribute(ATTRIBUTE_SRC);

        if (StringUtils.isEmpty(src) || LinkUtil.isExternalLink(src)) {
            return;
        }

        final String[] parts = src.split(PATH_SEPARATOR);
        final Node node = getNode();
        final String name = parts.length >= 1 ? parts[0] : null;
        final String uuid = FacetUtil.getChildDocBaseOrNull(node, name);

        if (uuid != null) {
            tag.addAttribute(ATTRIBUTE_SRC, imageURLProvider.getURL(src));
            tag.addAttribute(ATTRIBUTE_DATA_UUID, uuid);

            final String type = parts.length >= 3 ? parts[2] : null;
            if (type != null) {
                tag.addAttribute(ATTRIBUTE_DATA_TYPE, type);
            }
        }
    }

    private void convertImageForStorage(final Tag tag) throws RepositoryException {
        String src = tag.getAttribute(ATTRIBUTE_SRC);

        if (StringUtils.isEmpty(src)) {
            return;
        }

        final String uuid = tag.getAttribute(ATTRIBUTE_DATA_UUID);
        tag.removeAttribute(ATTRIBUTE_DATA_UUID);
        final String type = tag.getAttribute(ATTRIBUTE_DATA_TYPE);
        tag.removeAttribute(ATTRIBUTE_DATA_TYPE);

        if (uuid == null) {
            return;
        }

        final String name = findOrCreateFacetNode(uuid);
        if (name == null) {
            return;
        }
        src = name;
        if (type != null) {
            src += PATH_SEPARATOR + DOCUMENT_PATH_PLACEHOLDER + PATH_SEPARATOR + type;
        }
        tag.addAttribute(ATTRIBUTE_SRC, src);
    }
}
