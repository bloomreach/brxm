/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextLink;
import org.hippoecm.frontend.plugins.richtext.RichTextUtil;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrRichTextLinkFactory implements IRichTextLinkFactory {

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(JcrRichTextImageFactory.class);

    private IModel<Node> nodeModel;
    private transient Set<String> links = null;

    public JcrRichTextLinkFactory(IModel<Node> nodeModel) {
        this.nodeModel = nodeModel;
    }

    public RichTextLink loadLink(String uuid) throws RichTextException {
        if (Strings.isEmpty(uuid)) {
            throw new IllegalArgumentException("Link path is empty");
        }
        final Node node = nodeModel.getObject();
        try {
            final Item item = node.getSession().getNodeByIdentifier(uuid);
            final JcrNodeModel model = new JcrNodeModel(item.getPath());
            return new RichTextLink(model, uuid);
        } catch (RepositoryException e) {
            throw new RichTextException("Rich text link factory for node '" + JcrUtils.getNodePathQuietly(node) + "'"
                    + " cannot find linked node with UUID '" + uuid + "'", e);
        }
    }

    public RichTextLink createLink(final IModel<Node> targetModel) throws RichTextException {
        if (targetModel == null) {
            throw new IllegalArgumentException("Target is null");
        }
        try {
            final Node targetNode = targetModel.getObject();
            if (targetNode == null) {
                final String error = createTargetNodeDoesNotExistError(targetModel);
                throw new RichTextException(error);
            }
            final String name = NodeNameCodec.encode(targetNode.getName());
            final Node node = nodeModel.getObject();
            RichTextFacetHelper.createFacet(node, name, targetNode);
            final String targetUuid = targetNode.getIdentifier();
            return new RichTextLink(targetModel, targetUuid);
        } catch (RepositoryException e) {
            throw new RichTextException("could not create link", e);
        }
    }

    private static String createTargetNodeDoesNotExistError(final IModel<Node> targetModel) {
        String error = "Link target node does not exist";
        if (targetModel instanceof JcrNodeModel) {
            String path = ((JcrNodeModel)targetModel).getItemModel().getPath();
            error += ": '" + path + "'";
        }
        return error;
    }

    public boolean isValid(IModel<Node> targetModel) {
        if (targetModel == null) {
            return false;
        }
        Node node = targetModel.getObject();
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

    public Set<String> getLinkUuids() {
        if (links == null) {
            links = new TreeSet<String>();
            try {
                final NodeIterator iter = nodeModel.getObject().getNodes();
                while (iter.hasNext()) {
                    final Node child = iter.nextNode();
                    if (child.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        final String uuid = child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                        links.add(uuid);
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
     * @param uuids the current list of link UUIDs
     */
    public void cleanup(Set<String> uuids) {
        try {
            final Set<String> uuidsWithChildFacet = new TreeSet<String>();
            final NodeIterator iter = nodeModel.getObject().getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                if (child.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String uuid = child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    if (!uuids.contains(uuid) || uuidsWithChildFacet.contains(uuid)) {
                        child.remove();
                    } else {
                        uuidsWithChildFacet.add(uuid);
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
