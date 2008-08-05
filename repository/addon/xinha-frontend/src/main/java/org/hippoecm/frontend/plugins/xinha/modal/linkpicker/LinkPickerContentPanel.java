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
import java.util.EnumMap;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerContentPanel extends XinhaContentPanel<XinhaLink> {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(LinkPickerContentPanel.class);
    private final static String DEFAULT_JCR_PATH = "/content";
    private static final long serialVersionUID = 1L;
    
    private JcrNodeModel nodeModel;
    private String jcrPath = DEFAULT_JCR_PATH;
    private String uuid = null;
    private String htmlLink = null;

    public LinkPickerContentPanel(XinhaModalWindow modal, final JcrNodeModel nodeModel, EnumMap<XinhaLink, String> values) {
        super(modal, values);
        
        this.nodeModel = nodeModel;
        
        String jcrStartBrowsePath = null;
        
        String currentLink = values.get(XinhaLink.HREF);
        if(currentLink != null) {
            try {
                if(nodeModel.getNode().hasNode(currentLink)) {
                    Node currentLinkNode = nodeModel.getNode().getNode(currentLink);
                    if(currentLinkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        try {
                            htmlLink = currentLink;
                            uuid = currentLinkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                            Session jcrSession = currentLinkNode.getSession();
                            Item derefItem = jcrSession.getNodeByUUID(uuid);
                            if(derefItem.isNode()) {
                                Node deref = (Node)derefItem;
                                if(deref.isNodeType(HippoNodeType.NT_HANDLE)) {
                                    // get the parent node as start path
                                    jcrStartBrowsePath = deref.getParent().getPath();
                                    jcrPath = deref.getPath();
                                } else {
                                    log.error("docbase uuid does not refer to node of type hippo:handle: resetting link. uuid=" + uuid);
                                    htmlLink = null;
                                    uuid = null;
                                }
                            } else {
                                log.error("docbase uuid does not refer to node but property: resetting link. uuid=" + uuid);
                                htmlLink = null;
                                uuid = null;
                            }
                        } catch (ItemNotFoundException e) {
                            log.error("uuid in docbase not found: resetting link: " + uuid);
                            htmlLink = null;
                            uuid = null;
                        }
                        
                        
                    }
                }
            } catch (RepositoryException e) {
                log.error("error during nodetest for " + currentLink);
            }
        }
            
        // path location display
        final Label labelPath = new Label("jcrpath", new PropertyModel(this, "jcrPath"));
        form.add(labelPath);
        labelPath.setOutputMarkupId(true);
        //form.add(label);
        // ********************************************************************
        
        // node listing
        final List<NodeItem> items = getNodeItems(nodeModel, jcrStartBrowsePath);
        final WebMarkupContainer wrapper = new WebMarkupContainer("wrapper");
        ListView listing  = new ListView("item", items) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem item) {
                final NodeItem nodeItem = ((NodeItem) item.getModelObject()) ;

                final AjaxLink link = new AjaxLink("callback") {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        NodeItem nodeItem = ((NodeItem) item.getModelObject());
                        if(nodeItem.isHandle()) {
                            target.addComponent(ok.setEnabled(true));
                            htmlLink = nodeItem.getDisplayName();
                            uuid = nodeItem.getUuid();
                        } else {
                            List<NodeItem> newListing = getNodeItems(nodeModel, nodeItem.getPath());
                            items.clear();
                            items.addAll(newListing);
                            target.addComponent(ok.setEnabled(false));
                            target.addComponent(wrapper);
                            htmlLink = null;
                            uuid = null;
                        }
                        jcrPath = nodeItem.getPath();
                        target.addComponent(labelPath);
                        
                    }
                };
                link.add(new Label("linkname", nodeItem.getDisplayName()));
                
                final Label icon = new Label("icon") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if(nodeItem.isHandle()) {
                            tag.put("src", "skin/images/icons/document-16.png");
                        } else {
                            tag.put("src", "skin/images/icons/folder-16.png");
                        }
                        super.onComponentTag(tag);
                    }
                };
                link.add(icon);
                
                item.add(link);

            }
        };
        wrapper.add(listing);
        wrapper.setOutputMarkupId(true);
        form.add(wrapper);
        // ******************************************************************

    }

    @Override
    protected void onOk() {
        if (uuid == null) {
            log.error("uuid is null. Should never be possible for internal link");
            return;
        }
       
        Node node = nodeModel.getNode();
        
        /* test whether link is already present as facetselect. If true then:
         * 1) if uuid also same, use this link
         * 2) if uuid is different, create a new link 
         */ 
        
        HtmlLinkValidator htmlLinkValidator = new HtmlLinkValidator(node, htmlLink);
        
        values.put(XinhaLink.HREF, htmlLinkValidator.getValidLink());
        if(!htmlLinkValidator.isAlreadyPresent()) {
            try {
                Node facetselect = node.addNode(htmlLinkValidator.getValidLink(), HippoNodeType.NT_FACETSELECT);
                //todo fetch corresponding uuid of the chosen imageset
                facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
                facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
                facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
                facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
                // need a node save (the draft so no problem) to visualize images
                node.save();
            } catch (RepositoryException e) {
                log.error("An error occured while trying to save new image facetSelect[" + uuid + "]", e);
            }
        }
    }

    
    @Override
    protected void onDetach() {
        this.nodeModel.detach();
        super.onDetach();
    }
    
    @Override
    protected String getXinhaParameterName(XinhaLink k) {
        return k.getValue();
    }

    private List<NodeItem> getNodeItems(JcrNodeModel nodeModel, String startPath) {
        List<NodeItem> items = new ArrayList<NodeItem>();
        startPath = (startPath == null) ?  DEFAULT_JCR_PATH: startPath;
        try {
            Session session = nodeModel.getNode().getSession();
            Node rootNode = session.getRootNode();
            Node startNode = (Node) session.getItem(startPath);
            if(!startNode.isSame(rootNode) && !startNode.getParent().isSame(rootNode)){
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

    public class NodeItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String path;
        private String uuid;
        private String displayName;
        private boolean isHandle;
        
        public NodeItem(Node listNode) throws RepositoryException{
            this(listNode, null);
        }
        
        public NodeItem(Node listNode, String displayName) throws RepositoryException{
            this.path = listNode.getPath();
            this.displayName = (displayName==null)? listNode.getName():displayName;
            if(listNode.isNodeType("mix:referenceable")) {
                this.uuid = listNode.getUUID();
            }
            if(listNode.isNodeType(HippoNodeType.NT_HANDLE)){
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
    
    class HtmlLinkValidator {

        private String validLink;
        private boolean alreadyPresent; 
        
        public HtmlLinkValidator(Node node, String htmlLink) {
            visit(node, htmlLink, 0);
        }

        private void visit(Node node, String htmlLink, int postfix) {
            try {
                String testLink = htmlLink;
                if(postfix > 0) {
                    testLink +="_"+postfix;
                }
                if(node.hasNode(testLink)) {
                     Node htmlLinkNode = node.getNode(testLink);
                     if(htmlLinkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                       String docbase = htmlLinkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                       if(docbase.equals(uuid)) {
                           // we already have a link for this internal link, so reuse it
                           validLink = testLink;
                           alreadyPresent = true;
                       } else {
                           // we already have a link of this name, but points to different node, hence, try with another name
                           visit(node, testLink, ++postfix);
                           return;
                       }
                    }else {
                        // there is a node which is has the same name as the testLink, but is not a facetselect, try with another name
                        visit(node, testLink, ++postfix);
                        return;
                    }
                } else {
                    validLink = testLink;
                    alreadyPresent = false;
                }
            } catch (RepositoryException e) {
                log.error("error occured while saving internal link: " , e);
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
