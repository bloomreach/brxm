/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.util.FacetUtil;
import org.onehippo.cms7.services.htmlprocessor.util.JcrUtil;
import org.onehippo.cms7.services.htmlprocessor.util.StringUtil;
import org.onehippo.cms7.services.htmlprocessor.richtext.RichTextException;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLEncoder;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.NodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the creation and storage of RichTextImages in the JCR repository.
 */
public class RichTextImageFactoryImpl implements RichTextImageFactory {

    private final static Logger log = LoggerFactory.getLogger(RichTextImageFactoryImpl.class);

    private final Model<Node> nodeModel;
    private final NodeFactory nodeFactory;
    private final URLEncoder encoder;

    public RichTextImageFactoryImpl(final Model<Node> nodeModel, final NodeFactory nodeFactory) {
        this(nodeModel, nodeFactory, URLEncoder.OPAQUE);
    }

    public RichTextImageFactoryImpl(final Model<Node> nodeModel, final NodeFactory nodeFactory, final URLEncoder encoder) {
        this.nodeModel = nodeModel;
        this.nodeFactory = nodeFactory;
        this.encoder = encoder;
    }

    /**
     * Load an existing RichTextImage using the uuid and type.
     *
     * @throws RichTextException
     */
    @Override
    public RichTextImage loadImageItem(final String uuid, final String type) throws RichTextException {
        if (StringUtil.isEmpty(uuid)) {
            throw new IllegalArgumentException("uuid is empty");
        }
        try {
            final Node node = nodeFactory.getNodeByIdentifier(uuid);
            final String name = FacetUtil.getChildFacetNameOrNull(nodeModel.get(), uuid);
            return createImageItem(node, name, type);
        } catch (final ItemNotFoundException infe) {
            throw new RichTextException("Could not resolve " + uuid);
        } catch (final RepositoryException e) {
            log.error("Error resolving image " + uuid);
            throw new RichTextException("Error retrieving canonical node for imageNode[" + uuid + "]", e);
        }
    }

    @Override
    public RichTextImage createImageItem(final Model<Node> target) throws RichTextException {
        if (target == null) {
            throw new IllegalArgumentException("Target is null");
        }
        final Node node = target.get();
        return createImageItem(node, null, null);
    }

    public void save(final RichTextImage image) throws RichTextException {
        final Node node = this.nodeModel.get();
        final String imageName = image.getName();
        try {
            final Node imageNode = nodeFactory.getNodeByPath(image.getPath());
            final Node target = imageNode.getParent();
            final String uuid = target.getIdentifier();
            final String facetName = FacetUtil.createFacet(node, imageName, uuid);
            image.setName(facetName);
            image.setUuid(uuid);
        } catch (final RepositoryException e) {
            throw new RichTextException("Failed to create facet for image '" + imageName + "'", e);
        }
    }

    @Override
    public boolean isValid(final Model<Node> selectedModel) {
        if (selectedModel == null) {
            return false;
        }

        final Node node = selectedModel.get();
        if (node == null) {
            return false;
        }

        try {
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                final Node doc = node.getNode(node.getName());
                final Item primary = JcrUtil.getPrimaryItem(doc);
                return primary.isNode() && ((Node) primary).isNodeType(HippoNodeType.NT_RESOURCE);
            }
        } catch (ItemNotFoundException | PathNotFoundException e) {
            return false;
        } catch (final RepositoryException e) {
            log.error(e.getMessage());
        }

        return false;
    }

    /**
     * Create a RichTextImage that corresponds to the (physical) handle.
     *
     * @throws RichTextException
     */
    private RichTextImage createImageItem(Node node, String name, final String type) throws RichTextException {
        try {
            final Node root = node;
            final boolean isNew = name == null;
            if (isNew) {
                name = node.getName();
            }

            final NodeType nodetype = node.getPrimaryNodeType();
            if (nodetype.getName().equals(HippoNodeType.NT_HANDLE)) {
                node = node.getNode(node.getName());
            }

            final List<String> resourceDefinitions = new LinkedList<>();
            final Item primary = JcrUtil.getPrimaryItem(node);
            if (!primary.isNode() || !((Node) primary).isNodeType(HippoNodeType.NT_RESOURCE)) {
                throw new RichTextException("Invalid image document");
            }

            for (final NodeIterator children = node.getNodes(); children.hasNext(); ) {
                final Node child = children.nextNode();
                if (primary.isSame(child)) {
                    continue;
                }
                if (child.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    resourceDefinitions.add(child.getName());
                }
            }
            resourceDefinitions.add(primary.getName());
            if (resourceDefinitions.size() == 0) {
                throw new RichTextException("No child resource found");
            }

            final RichTextImage rti = new RichTextImage(node.getPath(), name, encoder);
            rti.setResourceDefinitions(resourceDefinitions);
            if (type != null) {
                rti.setSelectedResourceDefinition(type);
            } else {
                rti.setSelectedResourceDefinition(resourceDefinitions.get(0));
            }

            if (isNew) {
                save(rti);
            } else {
                rti.setUuid(root.getIdentifier());
            }
            return rti;
        } catch (final RepositoryException ex) {
            throw new RichTextException("Could not create image item", ex);
        }
    }

}
