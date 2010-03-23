/*
 *  Copyright 2008 Hippo.
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.PrependingStringBuffer;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextImage;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the creation and storage of RichTextImages in the JCR repository.
 */
public class JcrRichTextImageFactory implements IRichTextImageFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(JcrRichTextImageFactory.class);

    private JcrNodeModel nodeModel;

    public JcrRichTextImageFactory(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    /**
     * Load an existing RichTextImage, using the provided path.  The path is the relative
     * path to the facetselect that points to the handle of the image.
     */
    public RichTextImage loadImageItem(String path) {
        // find the nodename of the facetselect
        if (!Strings.isEmpty(path)) {
            try {
                javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
                Node root = nodeModel.getNode();
                Node node = ((HippoNode) workspace.getHierarchyResolver().getNode(root, path))
                        .getCanonicalNode();
                if (node != null) {
                    PrependingStringBuffer relativePathBuilder = new PrependingStringBuffer();
                    while (!node.equals(root) && !node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        node = node.getParent();
                        if (relativePathBuilder.length() > 0) {
                            relativePathBuilder.prepend('/');
                        }
                        relativePathBuilder.prepend(path.substring(path.lastIndexOf('/') + 1));
                        path = path.substring(0, path.lastIndexOf('/'));
                    }
                    return createImageItem(node, relativePathBuilder.toString());
                }
            } catch (PathNotFoundException e) {
                log.error("Error retrieving canonical node for imageNode[" + path + "]", e);
            } catch (RepositoryException e) {
                log.error("Error retrieving canonical node for imageNode[" + path + "]", e);
            }
        }
        return null;
    }

    public RichTextImage createImageItem(IDetachable target) {
        JcrNodeModel model = (JcrNodeModel) target;
        if (model != null) {
            try {
                Node node = model.getNode();
                return createImageItem(node, null);
            } catch (UnsupportedRepositoryOperationException e) {
                log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
            } catch (ItemNotFoundException e) {
                log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
            } catch (AccessDeniedException e) {
                log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
            } catch (RepositoryException e) {
                log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
            }
        }
        return null;
    }

    public boolean save(RichTextImage image) {
        Node node = nodeModel.getNode();
        try {
            String facet = RichTextFacetHelper.createFacet(node, image.getNodeName(), image.getUuid());
            if (facet != null && !facet.equals("")) {
                image.setFacetName(facet);
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Failed to create facet for " + image.getNodeName(), e);
        }
        return false;
    }

    public void delete(RichTextImage image) {
        if (image.getUuid() != null) {
            Node node = nodeModel.getNode();
            String facet = image.getFacetName();
            try {
                if (node.hasNode(facet)) {
                    Node imgNode = node.getNode(facet);
                    imgNode.remove();
                    node.getSession().save();
                }
            } catch (RepositoryException e) {
                log.error("An error occured while trying to save new image facetSelect[" + image.getNodeName() + "]",
                        e);
            }
        }
    }

    public boolean isValid(IDetachable targetId) {
        if (!(targetId instanceof JcrNodeModel)) {
            return false;
        }
        JcrNodeModel selectedModel = (JcrNodeModel) targetId;
        if (selectedModel == null) {
            return false;
        }
        Node node = selectedModel.getObject();
        if (node == null) {
            return false;
        }
        try {
            return node.isNodeType("mix:referenceable");
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    /**
     * Create a RichTextImage that corresponds to the (physical) handle.
     */
    private RichTextImage createImageItem(Node node, String selectedResource) throws UnsupportedRepositoryOperationException,
            ItemNotFoundException, AccessDeniedException, RepositoryException {

        NodeType nodetype = node.getPrimaryNodeType();
        if (nodetype.getName().equals("hippo:handle")) {
            node = node.getNode(node.getName());
            if (selectedResource != null) {
                selectedResource = selectedResource.substring(selectedResource.indexOf('/') + 1);
            }
        }

        List<String> resourceDefinitions = new ArrayList<String>();
        for (NodeDefinition nd : node.getPrimaryNodeType().getChildNodeDefinitions()) {
            if (!nd.getName().equals(node.getPrimaryItem().getName()) && nd.getDefaultPrimaryType() != null
                    && nd.getDefaultPrimaryType().isNodeType("hippo:resource")) {
                resourceDefinitions.add(nd.getName());
            }
        }
        RichTextImage rti = new RichTextImage(node.getPath(), node.getParent().getUUID(), node.getPrimaryItem().getName(), node
                .getName(), resourceDefinitions, nodeModel.getNode().getPath());
        if (selectedResource != null) {
            rti.setSelectedResourceDefinition(selectedResource);
        }
        save(rti);
        return rti;
    }

    public void detach() {
        nodeModel.detach();
    }

}
