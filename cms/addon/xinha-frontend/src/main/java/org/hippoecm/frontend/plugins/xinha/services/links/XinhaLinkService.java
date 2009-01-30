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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
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

    private String createLink(JcrNodeModel nodeModel) {
        try {
            return createLink(new NodeItem(nodeModel.getNode()));
        } catch (RepositoryException e) {
            log.error("Error creating NodeItem for nodeModel[" + nodeModel.getItemModel().getPath() + "]");
        }
        return null;
    }

    private String createLink(NodeItem item) {
        XinhaFacetHelper helper = new XinhaFacetHelper(false);
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

        private String path;
        private String uuid;
        private String displayName;
        private boolean isHandle;
        private String nodeName;

        public NodeItem(Node listNode) throws RepositoryException {
            this(listNode, null);
        }

        public NodeItem(Node listNode, String displayName) throws RepositoryException {
            this.path = listNode.getPath();
            if (displayName == null) {
                this.displayName = (String) new NodeTranslator(new JcrNodeModel(listNode)).getNodeName().getObject();
            } else {
                this.displayName = displayName;
            }
            if (listNode.isNodeType("mix:referenceable")) {
                this.uuid = listNode.getUUID();
            }
            if (listNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                isHandle = true;
            }
            this.nodeName = NodeNameCodec.encode(listNode.getName());
        }

        public String getNodeName() {
            return nodeName;
        }

        public String getUuid() {
            return uuid;
        }

        public String getPath() {
            return path;
        }

        public boolean isHandle() {
            return isHandle;
        }

        public String getDisplayName() {
            return displayName;
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
            String relPath = getHref();
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
