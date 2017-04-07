/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.util.FacetUtil;
import org.onehippo.cms7.services.processor.html.util.JcrUtil;
import org.onehippo.cms7.services.processor.html.util.StringUtil;
import org.onehippo.cms7.services.processor.richtext.RichTextException;
import org.onehippo.cms7.services.processor.richtext.UrlEncoder;
import org.onehippo.cms7.services.processor.richtext.jcr.NodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the creation and storage of RichTextImages in the JCR repository.
 */
public class RichTextImageFactoryImpl implements RichTextImageFactory {

    private final static Logger log = LoggerFactory.getLogger(RichTextImageFactoryImpl.class);

    private Model<Node> nodeModel;
    private NodeFactory nodeFactory;
    private UrlEncoder encoder;

    public RichTextImageFactoryImpl(Model<Node> nodeModel, final NodeFactory nodeFactory) {
        this(nodeModel, nodeFactory, UrlEncoder.OPAQUE);
    }

    public RichTextImageFactoryImpl(Model<Node> nodeModel, final NodeFactory nodeFactory, final UrlEncoder encoder) {
        this.nodeModel = nodeModel;
        this.nodeFactory = nodeFactory;
        this.encoder = encoder;
    }

    /**
     * Load an existing RichTextImage using the uuid and type.
     * @throws RichTextException
     */
    @Override
    public RichTextImage loadImageItem(String uuid, String type) throws RichTextException {
        if (StringUtil.isEmpty(uuid)) {
            throw new IllegalArgumentException("uuid is empty");
        }
        try {
            Node node = nodeFactory.getNodeByIdentifier(uuid);
            String name = FacetUtil.getChildFacetNameOrNull(nodeModel.get(), uuid);
            return createImageItem(node, name, type);
        } catch (ItemNotFoundException infe) {
            throw new RichTextException("Could not resolve " + uuid);
        } catch (RepositoryException e) {
            log.error("Error resolving image " + uuid);
            throw new RichTextException("Error retrieving canonical node for imageNode[" + uuid + "]", e);
        }
    }

    @Override
    public RichTextImage createImageItem(final Model<Node> target) throws RichTextException {
        if (target == null) {
            throw new IllegalArgumentException("Target is null");
        }
        Node node = target.get();
        return createImageItem(node, null, null);
    }

    public void save(RichTextImage image) throws RichTextException {
        final Node node = this.nodeModel.get();
        final String imageName = image.getName();
        try {
            final Node imageNode = nodeFactory.getNodeByPath(image.getPath());
            final Node target = imageNode.getParent();
            final String uuid = target.getIdentifier();
            final String facetName = FacetUtil.createFacet(node, imageName, uuid);
            image.setName(facetName);
            image.setUuid(uuid);
        } catch (RepositoryException e) {
            throw new RichTextException("Failed to create facet for image '" + imageName + "'", e);
        }
    }

    @Override
    public boolean isValid(final Model<Node> selectedModel) {
        if (selectedModel == null) {
            return false;
        }

        Node node = selectedModel.get();
        if (node == null) {
            return false;
        }

        try {
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                Node doc = node.getNode(node.getName());
                Item primary = JcrUtil.getPrimaryItem(doc);
                return primary.isNode() && ((Node) primary).isNodeType(HippoNodeType.NT_RESOURCE);
            }
        } catch (ItemNotFoundException | PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        return false;
    }

    /**
     * Create a RichTextImage that corresponds to the (physical) handle.
     *
     * @throws RichTextException
     */
    private RichTextImage createImageItem(Node node, String name, String type) throws RichTextException {
        try {
            Node root = node;
            boolean isNew = name == null;
            if (isNew) {
                name = node.getName();
            }

            NodeType nodetype = node.getPrimaryNodeType();
            if (nodetype.getName().equals(HippoNodeType.NT_HANDLE)) {
                node = node.getNode(node.getName());
            }

            List<String> resourceDefinitions = new LinkedList<>();
            Item primary = JcrUtil.getPrimaryItem(node);
            if (!primary.isNode() || !((Node) primary).isNodeType(HippoNodeType.NT_RESOURCE)) {
                throw new RichTextException("Invalid image document");
            }

            for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                Node child = children.nextNode();
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

            RichTextImage rti = new RichTextImage(node.getPath(), name, encoder);
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
        } catch (RepositoryException ex) {
            throw new RichTextException("Could not create image item", ex);
        }
    }

    @Override
    public void release() {
        nodeModel.release();
    }

}
