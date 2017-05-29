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
package org.onehippo.cms7.services.htmlprocessor.richtext.visit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.util.FacetUtil;
import org.onehippo.cms7.services.htmlprocessor.util.LinkUtil;
import org.onehippo.cms7.services.htmlprocessor.visit.FacetVisitor;

public class LinkVisitor extends FacetVisitor {

    public static final String TAG_A = "a";
    public static final String ATTRIBUTE_HREF = "href";

    public LinkVisitor(final Model<Node> nodeModel) {
        super(nodeModel);
    }

    @Override
    public void onRead(final Tag parent, final Tag tag) throws RepositoryException {
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_A, tag.getName())) {
            convertLinkForRetrieval(tag);
        }
    }

    @Override
    public void onWrite(final Tag parent, final Tag tag) throws RepositoryException {
        super.onWrite(parent, tag);
        if (tag != null && StringUtils.equalsIgnoreCase(TAG_A, tag.getName())) {
            convertLinkForStorage(tag);
        }
    }

    private void convertLinkForRetrieval(final Tag tag) throws RepositoryException {
        final String href = tag.getAttribute(ATTRIBUTE_HREF);

        if (StringUtils.isEmpty(href)) {
            return;
        }
        if (LinkUtil.isExternalLink(href)) {
            return;
        }

        final Node node = getNode();
        final String name = NodeNameCodec.encode(href, true);
        final String uuid = FacetUtil.getChildDocBaseOrNull(node, name);

        if (uuid != null) {
            tag.addAttribute(ATTRIBUTE_HREF, LinkUtil.INTERNAL_LINK_DEFAULT_HREF);
            tag.addAttribute(ATTRIBUTE_DATA_UUID, uuid);
        }
    }

    private void convertLinkForStorage(final Tag tag) throws RepositoryException {
        final String href = tag.getAttribute(ATTRIBUTE_HREF);

        if (StringUtils.isEmpty(href)) {
            return;
        }

        final String uuid = tag.getAttribute(ATTRIBUTE_DATA_UUID);
        tag.removeAttribute(ATTRIBUTE_DATA_UUID);

        if (LinkUtil.isExternalLink(href)) {
            return;
        }

        if (uuid == null) {
            return;
        }

        final String name = findOrCreateFacetNode(uuid);
        if (name != null) {
            tag.addAttribute(ATTRIBUTE_HREF, name);
        } else {
            tag.removeAttribute(ATTRIBUTE_HREF);
        }
    }
}
