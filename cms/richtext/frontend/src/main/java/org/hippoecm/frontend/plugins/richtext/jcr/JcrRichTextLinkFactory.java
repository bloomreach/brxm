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
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
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

    public RichTextLink loadLink(String relPath) throws RichTextException {
        if (Strings.isEmpty(relPath)) {
            throw new IllegalArgumentException("Link path is empty");
        }
        relPath = RichTextUtil.decode(relPath);
        try {
            Node node = nodeModel.getNode();
            Node linkNode = node.getNode(relPath);
            if (linkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                String uuid = linkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                Item item = node.getSession().getNodeByUUID(uuid);
                if (item != null) {
                    JcrNodeModel model = new JcrNodeModel(item.getPath());
                    return new RichTextLink(model, relPath);
                } else {
                    throw new RichTextException("Facetselect points to non-existing uuid" + uuid);
                }
            } else {
                throw new RichTextException("Found node is not a facetselect");
            }
        } catch (PathNotFoundException e) {
            throw new RichTextException("Error finding facet node for relative path " + relPath, e);
        } catch (RepositoryException e) {
            throw new RichTextException("Error finding facet node for relative path " + relPath, e);
        }
    }

    public RichTextLink createLink(IDetachable targetId) throws RichTextException {
        JcrNodeModel targetModel = (JcrNodeModel) targetId;
        if (targetModel == null) {
            throw new IllegalArgumentException("Target is null");
        }
        try {
            Node targetNode = targetModel.getNode();
            if (targetNode == null) {
                throw new RichTextException("Node does not exist at " + targetModel.getItemModel().getPath());
            }
            String name = NodeNameCodec.encode(targetNode.getName());
            Node node = nodeModel.getNode();
            name = RichTextFacetHelper.createFacet(node, name, targetNode);
            return new RichTextLink(targetModel, name);
        } catch (RepositoryException e) {
            throw new RichTextException("could not create link", e);
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
                log.error("Error retrieving links", ex);
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
