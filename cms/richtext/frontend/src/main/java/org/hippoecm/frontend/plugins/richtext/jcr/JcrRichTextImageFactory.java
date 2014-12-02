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
package org.hippoecm.frontend.plugins.richtext.jcr;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImage;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the creation and storage of RichTextImages in the JCR repository.
 */
public class JcrRichTextImageFactory implements IRichTextImageFactory {

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(JcrRichTextImageFactory.class);

    private IModel<Node> nodeModel;

    public JcrRichTextImageFactory(IModel<Node> nodeModel) {
        this.nodeModel = nodeModel;
    }

    /**
     * Load an existing RichTextImage using the uuid and type.
     * @throws RichTextException
     */
    @Override
    public RichTextImage loadImageItem(String uuid, String type) throws RichTextException {
        if (Strings.isEmpty(uuid)) {
            throw new IllegalArgumentException("uuid is empty");
        }
        try {
            javax.jcr.Session session = UserSession.get().getJcrSession();
            try {
                Node node = session.getNodeByIdentifier(uuid);
                String name = RichTextFacetHelper.getChildFacetNameOrNull(nodeModel.getObject(), uuid);
                return createImageItem(node, name, type);
            } catch (ItemNotFoundException infe) {
                throw new RichTextException("Could not resolve " + uuid);
            }
        } catch (RepositoryException e) {
            log.error("Error resolving image " + uuid);
            throw new RichTextException("Error retrieving canonical node for imageNode[" + uuid + "]", e);
        }
    }

    public RichTextImage createImageItem(IDetachable target) throws RichTextException {
        JcrNodeModel model = (JcrNodeModel) target;
        if (model == null) {
            throw new IllegalArgumentException("Target is null");
        }
        Node node = model.getNode();
        return createImageItem(node, null, null);
    }

    public void save(RichTextImage image) throws RichTextException {
        final Node node = this.nodeModel.getObject();
        final String imageName = image.getName();
        try {
            final Node target = image.getTarget().getObject();
            final String uuid = target.getIdentifier();
            final String facetName = RichTextFacetHelper.createFacet(node, imageName, uuid);
            image.setName(facetName);
            image.setUuid(uuid);
        } catch (RepositoryException e) {
            throw new RichTextException("Failed to create facet for image '" + imageName + "'", e);
        }
    }

    public boolean isValid(IDetachable targetId) {
        if (!(targetId instanceof JcrNodeModel)) {
            return false;
        }
        JcrNodeModel selectedModel = (JcrNodeModel) targetId;
        Node node = selectedModel.getObject();
        if (node == null) {
            return false;
        }
        try {
            if (node.isNodeType("hippo:handle")) {
                Node doc = node.getNode(node.getName());
                Item primary = JcrHelper.getPrimaryItem(doc);
                return (primary.isNode() && ((Node) primary).isNodeType("hippo:resource"));
            }
        } catch (ItemNotFoundException infe) {
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
            boolean isNew = name == null;
            if (isNew) {
                name = node.getName();
            }

            NodeType nodetype = node.getPrimaryNodeType();
            if (nodetype.getName().equals("hippo:handle")) {
                node = node.getNode(node.getName());
            }

            List<String> resourceDefinitions = new LinkedList<String>();
            Item primary = JcrHelper.getPrimaryItem(node);
            if (!primary.isNode() || !((Node) primary).isNodeType("hippo:resource")) {
                throw new RichTextException("Invalid image document");
            }

            for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                Node child = children.nextNode();
                if (primary.isSame(child)) {
                    continue;
                }
                if (child.isNodeType("hippo:resource")) {
                    resourceDefinitions.add(child.getName());
                }
            }
            resourceDefinitions.add(primary.getName());
            if (resourceDefinitions.size() == 0) {
                throw new RichTextException("No child resource found");
            }

            RichTextImage rti = new RichTextImage(node.getPath(), name);
            rti.setResourceDefinitions(resourceDefinitions);
            if (type != null) {
                rti.setSelectedResourceDefinition(type);
            } else {
                rti.setSelectedResourceDefinition(resourceDefinitions.get(0));
            }

            if (isNew) {
                save(rti);
            }
            return rti;
        } catch (RepositoryException ex) {
            throw new RichTextException("Could not create image item", ex);
        }
    }

    public void detach() {
        nodeModel.detach();
    }

}
