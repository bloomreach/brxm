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

package org.hippoecm.frontend.plugins.xinha.modal.linkpicker;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.linkpicker.LinkPickerContentPanel.NodeItem;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalLinkDAO  implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(InternalLinkDAO.class);

    private JcrNodeModel model;

    public InternalLinkDAO(JcrNodeModel model) {
        this.model = model;
    }

    public String create(String name, String uuid) {
        if (uuid == null) {
            log.error("uuid is null. Should never be possible for internal link");
            return "";
        }

        Node node = model.getNode();

        /* test whether link is already present as facetselect. If true then:
         * 1) if uuid also same, use this link
         * 2) if uuid is different, create a new link
         */

        HtmlLinkValidator htmlLinkValidator = new HtmlLinkValidator(node, name, uuid);
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

    public boolean remove(String initialHref) {
        try {
            Node node = model.getNode();
            Node facet = node.getNode(initialHref);
            facet.remove();
            node.save();
            return true;
        } catch (PathNotFoundException e) {
            log.warn("Internal link[" + initialHref + "] not found in node["
                    + model.getItemModel().getPath() + "]");
        } catch (RepositoryException e) {
            log.error("An error occured while removing internal link[" + initialHref + "] from node["
                    + model.getItemModel().getPath() + "]", e);
        }
        return false;
    }

    public String getUUID(String currentLink) throws RepositoryException {
        if (model.getNode().hasNode(currentLink)) {
            Node currentLinkNode = model.getNode().getNode(currentLink);
            if (currentLinkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                return currentLinkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
            }
        }
        throw new RepositoryException("Facet[" + currentLink + "] not found");
    }

    public Item getItem(String uuid) throws RepositoryException {
        return model.getNode().getSession().getNodeByUUID(uuid);
    }

    public List<NodeItem> getNodeItems(String startPath) {
        List<NodeItem> items = new ArrayList<NodeItem>();
        startPath = (startPath == null) ? LinkPickerContentPanel.DEFAULT_JCR_PATH : startPath;
        try {
            Session session = model.getNode().getSession();
            Node rootNode = session.getRootNode();
            Node startNode = (Node) session.getItem(startPath);
            if (!startNode.isSame(rootNode) && !startNode.getParent().isSame(rootNode)) {
                items.add(new NodeItem(startNode.getParent(), "[..]"));
            }
            NodeIterator listingNodesIt = startNode.getNodes();
            while (listingNodesIt.hasNext()) {
                HippoNode listNode = (HippoNode) listingNodesIt.nextNode();
                // nextNode can return null
                if (listNode == null) {
                    continue;
                }
                items.add(new NodeItem(listNode));
            }
        } catch (PathNotFoundException e) {
            // possible for old links
            log.warn("path not found : " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage(), e);
        }
        return items;
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
