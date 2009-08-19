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

package org.hippoecm.frontend.plugins.xinha.services.links;

import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.XinhaUtil;
import org.hippoecm.frontend.plugins.xinha.services.XinhaFacetHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XinhaLinkService implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(XinhaLinkService.class);

    private JcrNodeModel nodeModel;

    public XinhaLinkService(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public InternalXinhaLink create(Map<String, String> p) {
        return new InternalLink(p, nodeModel);
    }

    public String attach(JcrNodeModel model) {
        String href = createLink(model);
        if (href != null) {
            String script = "xinha_editors." + getXinhaName() + ".plugins.CreateLink.instance.createLink({"
                    + XinhaLink.HREF + ": '" + href + "', " + XinhaLink.TARGET + ": ''}, false);";
            return script;
        }
        return null;
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
    
    private String createLink(JcrNodeModel nodeModel) {
        try {
            String link = createLink(new NodeItem(nodeModel.getNode()));
            return XinhaUtil.encode(link);
        } catch (RepositoryException e) {
            log.error("Error creating NodeItem for nodeModel[" + nodeModel.getItemModel().getPath() + "]");
        }
        return null;
    }

    private String createLink(NodeItem item) {
        XinhaFacetHelper helper = new XinhaFacetHelper();
        Node node = nodeModel.getNode();
        try {
            return helper.createFacet(node, item.getNodeName(), item.getUuid());
        } catch (RepositoryException e) {
            log.error("Failed to create facet for " + item.getNodeName(), e);
        }
        return "";
    }

    private class NodeItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String uuid;
        private String nodeName;

        public NodeItem(Node listNode) throws RepositoryException {
            this(listNode, null);
        }

        public NodeItem(Node listNode, String displayName) throws RepositoryException {
            if (listNode.isNodeType("mix:referenceable")) {
                this.uuid = listNode.getUUID();
            }
            this.nodeName = NodeNameCodec.encode(listNode.getName());
        }

        public String getNodeName() {
            return nodeName;
        }

        public String getUuid() {
            return uuid;
        }
    }

    private class InternalLink extends InternalXinhaLink {
        private static final long serialVersionUID = 1L;


        public InternalLink(Map<String, String> values, JcrNodeModel parentModel) {
            super(values, parentModel);
        }

        public void save() {
            if (isAttacheable()) {
                if (isReplacing()) {
                    new InternalLink(getInitialValues(), nodeModel).delete();
                }
                String url = createLink(getNodeModel());
                if (url != null) {
                    setHref(url);
                }
            }
        }

        public void delete() {
            String relPath = XinhaUtil.decode(getHref());
            Node node = nodeModel.getNode();
            try {
                if (node.hasNode(relPath)) {
                    Node linkNode = node.getNode(relPath);
                    linkNode.remove();
                    node.getSession().save();
                    setHref(null);
                }
            } catch (RepositoryException e) {
                log.error("Error during remove of internal link node[" + relPath + "]", e);
            }
        }

    }

    protected abstract String getXinhaName();

}
