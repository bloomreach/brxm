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
import org.hippoecm.repository.api.HippoNodeType;
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
     * Load an existing RichTextImage, using the provided path.  The path is the relative
     * path to the facetselect that points to the handle of the image.
     * @throws RichTextException 
     */
    public RichTextImage loadImageItem(String path) throws RichTextException {
        // find the nodename of the facetselect
        if (Strings.isEmpty(path)) {
            throw new IllegalArgumentException("path is empty");
        }
        try {
            String name = path;
            String relPath = null;
            if (path.indexOf('/') > 0) {
                name = path.substring(0, path.indexOf('/'));
                relPath = path.substring(path.indexOf('/') + 1);
            }
            javax.jcr.Session session = UserSession.get().getJcrSession();
            Node root = nodeModel.getObject();
            if (root.hasNode(name)) {
                Node link = root.getNode(name);
                if (link.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String uuid = link.getProperty("hippo:docbase").getString();
                    try {
                        Node node = session.getNodeByIdentifier(uuid);
                        return createImageItem(node, name, relPath);
                    } catch (ItemNotFoundException infe) {
                        throw new RichTextException("Could not resolve " + uuid);
                    }
                }
            }
            log.error("Link {} does not correspond to a valid facetselect", path);
            throw new RichTextException("Canonical node is null at relative path " + path);
        } catch (RepositoryException e) {
            log.error("Error resolving image " + path);
            throw new RichTextException("Error retrieving canonical node for imageNode[" + path + "]", e);
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

    public boolean save(RichTextImage image) throws RichTextException {
        Node node = nodeModel.getObject();
        try {
            Node target = ((JcrNodeModel) image.getTarget()).getNode();
            String facet = RichTextFacetHelper.createFacet(node, image.getName(), target);
            if (facet != null && !facet.equals("")) {
                image.setName(facet);
                return true;
            }
        } catch (RepositoryException e) {
            throw new RichTextException("Failed to create facet for " + image.getName(), e);
        }
        return false;
    }

    public boolean isValid(IDetachable targetId, String facetSelectPath) {
        if (!isValid(targetId)) {
            return false;
        }
        if (facetSelectPath == null) {
            return false;
        }
        if (facetSelectPath.indexOf('/') <= 0) {
            return false;
        }
        String docsub = facetSelectPath.substring(facetSelectPath.indexOf('/') + 1);
        if (!docsub.startsWith("{_document}/")) {
            return false;
        }
        String resource = docsub.substring(docsub.indexOf('/') + 1);
        if (resource.indexOf('/') > 0) {
            return false;
        }

        JcrNodeModel selectedModel = (JcrNodeModel) targetId;
        Node node = selectedModel.getObject();
        try {
            node = node.getNode(node.getName());
            return node.hasNode(resource);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    private boolean isValid(IDetachable targetId) {
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

    public String getDefaultFacetSelectPath(IDetachable targetId) {
        if (!isValid(targetId)) {
            return null;
        }
        JcrNodeModel selectedModel = (JcrNodeModel) targetId;
        Node node = selectedModel.getObject();
        try {
            if (node.isNodeType("hippo:handle")) {
                node = node.getNode(node.getName());
                Item primary = JcrHelper.getPrimaryItem(node);
                for (NodeIterator children = node.getNodes(); children.hasNext();) {
                    Node child = children.nextNode();
                    if (primary.isSame(child)) {
                        continue;
                    }
                    if (child.isNodeType("hippo:resource")) {
                        return node.getName() + "/{_document}/" + child.getName();
                    }
                }
                return node.getName() + "/{_document}/" + primary.getName();
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Create a RichTextImage that corresponds to the (physical) handle.
     * @throws RichTextException 
     */
    private RichTextImage createImageItem(Node node, String name, String selectedResource) throws RichTextException {
        try {
            boolean isNew = name == null;
            if (isNew) {
                name = node.getName();
            }

            NodeType nodetype = node.getPrimaryNodeType();
            if (nodetype.getName().equals("hippo:handle")) {
                node = node.getNode(node.getName());
                if (selectedResource != null) {
                    selectedResource = selectedResource.substring(selectedResource.indexOf('/') + 1);
                }
            }

            List<String> resourceDefinitions = new LinkedList<String>();
            Item primary = JcrHelper.getPrimaryItem(node);
            if (!primary.isNode() || !((Node) primary).isNodeType("hippo:resource")) {
                throw new RichTextException("Invalid image document");
            }

            for (NodeIterator children = node.getNodes(); children.hasNext();) {
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
            if (selectedResource != null) {
                rti.setSelectedResourceDefinition(selectedResource);
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
