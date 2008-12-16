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

package org.hippoecm.frontend.plugins.xinha;

import java.util.HashMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin.Configuration;
import org.hippoecm.frontend.plugins.xinha.dialog.JsBean;
import org.hippoecm.frontend.plugins.xinha.dialog.links.XinhaLink;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaLinkService implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(XinhaLinkService.class);

    private Configuration configuration;
    private JcrNodeModel nodeModel;

    public XinhaLinkService(Configuration configuration, JcrNodeModel nodeModel) {
        this.configuration = configuration;
        this.nodeModel = nodeModel;
    }

    public JsBean create(HashMap<String, String> p) {
        XinhaLink link = new XinhaLink(p);
        link.setNodeModel(findModel(link.getHref()));
        return link;
    }

    public String attach(XinhaLink link) {
        return createLink(link.getNodeModel());
    }

    public String attach(JcrNodeModel model) {
        String href = createLink(model);
        if (href != null) {
            String script = "xinha_editors." + configuration.getName() + ".plugins.CreateLink.instance.createLink({"
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
        if (item.getUuid() == null) {
            log.error("uuid is null. Should never be possible for internal link");
            return "";
        }

        Node node = nodeModel.getNode();
        String uuid = item.getUuid();
        String linkName = item.getDisplayName();

        /* test whether link is already present as facetselect. If true then:
         * 1) if uuid also same, use this link
         * 2) if uuid is different, create a new link
         */

        HtmlLinkValidator htmlLinkValidator = new HtmlLinkValidator(node, linkName, uuid);
        String validLink = htmlLinkValidator.getValidLink();
        if (!htmlLinkValidator.isAlreadyPresent()) {
            try {
                Node facetselect = node.addNode(validLink, HippoNodeType.NT_FACETSELECT);
                //todo fetch corresponding uuid of the chosen imageset
                facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
                facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
                facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
                facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
                // need a node save (the draft so no problem) to visualize images
                node.save();
            } catch (RepositoryException e) {
                log.error("An error occured while trying to save new image facetSelect[" + uuid + "]", e);
                validLink = "";
            }
        }
        return validLink;
    }

    private JcrNodeModel findModel(String relPath) {
        if (relPath != null) {
            relPath = XinhaUtil.decode(relPath);
            try {
                Node node = nodeModel.getNode();
                if (node.hasNode(relPath)) {
                    Node linkNode = node.getNode(relPath);
                    if (linkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String uuid = linkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                        Item item = node.getSession().getNodeByUUID(uuid);
                        if (item != null) {
                            return new JcrNodeModel(item.getPath());
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

    private class NodeItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String path;
        private String uuid;
        private String displayName;
        private boolean isHandle;

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

    private static class HtmlLinkValidator {

        private String validLink;
        private boolean alreadyPresent;

        public HtmlLinkValidator(Node node, String link, String uuid) {
            visit(node, link, uuid, 0);
        }

        private void visit(Node node, String link, String uuid, int postfix) {
            try {
                String testLink = link;
                if (postfix > 0) {
                    testLink += "_" + postfix;
                }
                if (node.hasNode(testLink)) {
                    Node htmlLinkNode = node.getNode(testLink);
                    if (htmlLinkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String docbase = htmlLinkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                        if (docbase.equals(uuid)) {
                            // we already have a link for this internal link, so reuse it
                            validLink = testLink;
                            alreadyPresent = true;
                        } else {
                            // we already have a link of this name, but points to different node, hence, try with another name
                            visit(node, testLink, uuid, ++postfix);
                            return;
                        }
                    } else {
                        // there is a node which is has the same name as the testLink, but is not a facetselect, try with another name
                        visit(node, testLink, uuid, ++postfix);
                        return;
                    }
                } else {
                    validLink = testLink;
                    alreadyPresent = false;
                }
            } catch (RepositoryException e) {
                log.error("error occured while saving internal link: ", e);
            }
        }

        public String getValidLink() {
            return validLink;
        }

        public boolean isAlreadyPresent() {
            return alreadyPresent;
        }
    }

}
