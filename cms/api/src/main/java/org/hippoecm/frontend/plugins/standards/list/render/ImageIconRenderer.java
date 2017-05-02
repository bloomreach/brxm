/*
 * Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.list.render;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;

public class ImageIconRenderer extends IconRenderer {

    private final double ratio;

    public ImageIconRenderer(final int thumbnailSize, final int boxSize) {
        ratio = thumbnailSize / (double) boxSize;
    }

    @Override
    protected HippoIcon getIcon(final String id, final Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            final String nodeName = node.getName();
            if (node.hasNode(nodeName)) {
                final Node imageSet = node.getNode(nodeName);
                // Thumbnail is marked as primary by default
                final Item primaryItem = JcrHelper.getPrimaryItem(imageSet);
                if (primaryItem.isNode()) {
                    final Node imageNode = (Node) primaryItem;
                    if (imageNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                        int iconWidth = -1;
                        int iconHeight = -1;

                        if (imageNode.hasProperty(HippoGalleryNodeType.IMAGE_WIDTH)) {
                            final double storedWidth = imageNode.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).getDouble();
                            iconWidth = (int) (storedWidth / ratio);

                        }

                        if (imageNode.hasProperty(HippoGalleryNodeType.IMAGE_HEIGHT)) {
                            final double storedHeight = imageNode.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).getDouble();
                            iconHeight = (int) (storedHeight / ratio);
                        }

                        final JcrResourceStream stream = new JcrResourceStream(new JcrNodeModel(imageNode));
                        return HippoIcon.fromStream(id, Model.of(stream), iconWidth, iconHeight);
                    }
                }
            }
        }
        return super.getIcon(id, node);
    }
}
