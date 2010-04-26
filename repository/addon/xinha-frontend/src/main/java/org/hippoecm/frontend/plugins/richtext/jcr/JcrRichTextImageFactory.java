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

import java.util.LinkedList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImage;
import org.hippoecm.frontend.session.UserSession;
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
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
            Node root = nodeModel.getNode();
            if (root.hasNode(name)) {
                Node link = root.getNode(name);
                if (link.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String uuid = link.getProperty("hippo:docbase").getString();
                    Node node = session.getNodeByUUID(uuid);
                    return createImageItem(node, name, relPath);
                }
            }
            throw new RichTextException("Canonical node is null at relative path " + path);
        } catch (RepositoryException e) {
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
        Node node = nodeModel.getNode();
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

    public void delete(RichTextImage image) {
        Node node = nodeModel.getNode();
        String facet = image.getName();
        try {
            if (node.hasNode(facet)) {
                Node imgNode = node.getNode(facet);
                imgNode.remove();
                node.getSession().save();
            }
        } catch (RepositoryException e) {
            log.error("An error occured while trying to save new image facetSelect[" + image.getName() + "]", e);
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
            return !node.isNodeType("hippostd:folder");
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return false;
        }
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

            RichTextImage rti = new RichTextImage(node.getPath(), name);

            String primaryItemDefinition = node.getPrimaryItem().getName();
            List<String> resourceDefinitions = new LinkedList<String>();
            resourceDefinitions.add(primaryItemDefinition);
            for (NodeDefinition nd : node.getPrimaryNodeType().getChildNodeDefinitions()) {
                if (!nd.getName().equals(primaryItemDefinition) && nd.getDefaultPrimaryType() != null
                        && nd.getDefaultPrimaryType().isNodeType("hippo:resource")) {
                    resourceDefinitions.add(nd.getName());
                }
            }
            rti.setResourceDefinitions(resourceDefinitions);
            if (selectedResource != null) {
                rti.setSelectedResourceDefinition(selectedResource);
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
