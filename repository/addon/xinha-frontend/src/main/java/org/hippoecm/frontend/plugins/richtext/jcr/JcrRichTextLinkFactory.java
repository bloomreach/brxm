/*
 *  Copyright 2010 Hippo.
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

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextLink;
import org.hippoecm.frontend.plugins.richtext.RichTextUtil;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrRichTextLinkFactory implements IRichTextLinkFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(JcrRichTextImageFactory.class);

    private JcrNodeModel nodeModel;
    private transient Set<String> links = null;

    public JcrRichTextLinkFactory(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public RichTextLink loadLink(String relPath) {
        if (relPath != null && !"".equals(relPath)) {
            relPath = RichTextUtil.decode(relPath);
            try {
                Node node = nodeModel.getNode();
                if (node.hasNode(relPath)) {
                    Node linkNode = node.getNode(relPath);
                    if (linkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String uuid = linkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                        Item item = node.getSession().getNodeByUUID(uuid);
                        if (item != null) {
                            JcrNodeModel model = new JcrNodeModel(item.getPath());
                            return new RichTextLink(model, relPath);
                        }
                    }
                }
            } catch (PathNotFoundException e) {
                log.error("Error finding facet node for relative path " + relPath, e);
            } catch (RepositoryException e) {
                log.error("Error finding facet node for relative path " + relPath, e);
            }
        }
        return null;
    }

    public RichTextLink createLink(IDetachable targetId) {
        JcrNodeModel targetModel = (JcrNodeModel) targetId;
        if (targetModel != null && targetModel.getNode() != null) {
            try {
                Node targetNode = targetModel.getNode();
                String name = NodeNameCodec.encode(targetNode.getName());
                Node node = nodeModel.getNode();
                String uuid = targetNode.getUUID();
                name = RichTextFacetHelper.createFacet(node, name, uuid);
                return new RichTextLink(targetModel, name);
            } catch (RepositoryException e) {
                log.error("could not create link", e);
            }
        }
        return null;
    }

    public void delete(RichTextLink link) {
        try {
            nodeModel.getNode().getNode(link.getName()).remove();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
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

    public Set<String> getLinks() {
        if (links == null) {
            links = new TreeSet<String>();
            try {
                NodeIterator iter = nodeModel.getNode().getNodes();
                while (iter.hasNext()) {
                    Node child = iter.nextNode();
                    if (child.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String name = child.getName();
                        links.add(name);
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Error removing unused links", ex);
            }
        }
        return links;
    }

    /**
     * Remove any facetselects that are no longer used.
     * 
     * @param references the current list of link names 
     */
    public void cleanup(Set<String> references) {
        try {
            NodeIterator iter = nodeModel.getNode().getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                if (child.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String name = child.getName();
                    if (!references.contains(name)) {
                        child.remove();
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error("Error removing unused links", ex);
        }
    }

    public void detach() {
        nodeModel.detach();
        links = null;
    }

}
