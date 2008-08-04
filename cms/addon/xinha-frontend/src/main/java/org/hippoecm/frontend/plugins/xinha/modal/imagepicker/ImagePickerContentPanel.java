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
package org.hippoecm.frontend.plugins.xinha.modal.imagepicker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagePickerContentPanel extends XinhaContentPanel<XinhaImage> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(ImagePickerContentPanel.class);

    private static final long serialVersionUID = 1L;

    private FeedbackPanel feedback;
    private JcrNodeModel nodeModel;
    private String uuid = null;
    private String urlValue = null;
    private String selectedimg = "/skin/images/empty.gif";
    
    /*
     * Image resource nodes various formats
     */ 
    private String selectedRN;
   

    public ImagePickerContentPanel(XinhaModalWindow modal, JcrNodeModel nodeModel,final EnumMap<XinhaImage, String> values) {
        super(modal, values);

        this.nodeModel = nodeModel;

        ok.setEnabled(false);
        form.add(feedback = new FeedbackPanel("feedback"));
        feedback.setOutputMarkupId(true);
        
        urlValue = values.get(XinhaImage.URL);
       
        if(urlValue != null && urlValue.startsWith("binaries")) {
            // find the nodename of the facetselect
            String resourcePath = urlValue.substring("binaries".length());
            Item resourceItem;
            try {
                resourceItem = nodeModel.getNode().getSession().getItem(resourcePath);
                if(resourceItem.isNode() && ((Node)resourceItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                    // now get the facetselect
                    Node facetSelect = resourceItem.getParent().getParent();
                    values.put(XinhaImage.URL, facetSelect.getName());
                    // and get the thumbnail
                    selectedimg = "binaries" + resourceItem.getParent().getPath()+"/"+resourceItem.getParent().getPrimaryItem().getName();
                }
            } catch (PathNotFoundException e) {
                log.warn("resourcePath not found: " + resourcePath);
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            
        }
        // input text field for the image name, which also becomes a facetselect 
        final TextField url = new TextField("url", newEnumModel(XinhaImage.URL));
        url.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                urlValue = url.getInput();
                if (isValidName(urlValue) && uuid != null) {
                    target.addComponent(ok.setEnabled(true));
                } else {
                    target.addComponent(ok.setEnabled(false));
                }
                values.put(XinhaImage.URL,url.getInput());
                target.addComponent(feedback);
            }
        });
        url.setEnabled(false);
        form.add(url);
        // ******************************************************************
        
        // Image resource nodes various formats
        final List<String> resourceNodes = new ArrayList<String>();
        final FormComponent dropdown = new DropDownChoice("resourcenodes", new PropertyModel(this, "selectedRN"), resourceNodes) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }

            @Override
            protected void onSelectionChanged(Object newSelection) {
                selectedRN = ((String) newSelection);
            }
        };
        dropdown.setOutputMarkupId(true);
        dropdown.setEnabled(false);
        
        dropdown.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if(selectedRN != null) {
                    target.addComponent(ok.setEnabled(true));
                }
            }
        });
        form.add(dropdown);

        // ******************************************************************
        
        // preview of the selected image
        final Label selectedLabel = new Label("selectedimg") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("src", selectedimg);
                tag.put("width", "50");
                super.onComponentTag(tag);
            }
        };
        selectedLabel.setOutputMarkupId(true);
        form.add(selectedLabel);
        // ******************************************************************
        
        // image listing
        final List<ImageItem> items = getImageItems(nodeModel);
        form.add(new ListView("item", items) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem item) {

                WebMarkupContainer entry = new WebMarkupContainer("entry");
                entry.add(new Label("thumbnail") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.put("src", ((ImageItem) item.getModelObject()).getPrimaryUrl());
                        tag.put("width", "50");
                        super.onComponentTag(tag);
                    }

                });

                final AjaxLink link = new AjaxLink("callback") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ImageItem imgItem = ((ImageItem) item.getModelObject()) ;
                        uuid = imgItem.getUuid();
                        selectedimg = imgItem.getPrimaryUrl();
                        target.addComponent(selectedLabel);

                        resourceNodes.clear();
                        List<String> resourceDefinitions = imgItem.getResourceDefinitions();
                        boolean needsToChoose = false;
                        if(resourceDefinitions.size() > 1) {
                            resourceNodes.addAll(resourceDefinitions);
                            dropdown.setEnabled(true);
                            target.addComponent(dropdown);
                            selectedRN = null;
                            needsToChoose = true;
                        }
                        else if(dropdown.isEnabled()){
                            dropdown.setEnabled(false);
                            target.addComponent(dropdown);
                        } 
                        
                        if(resourceDefinitions.size() == 1) {
                            selectedRN = resourceDefinitions.get(0);
                        } 
                        if(resourceDefinitions.size() == 0) {
                            selectedRN = null;
                        } 
                        
                         values.put(XinhaImage.URL, imgItem.getNodeName());
                         urlValue = imgItem.getNodeName();
                         target.addComponent(url);
                        
                        if (isValidName(urlValue) && uuid != null && !needsToChoose) {
                            target.addComponent(ok.setEnabled(true));
                        } else {
                            target.addComponent(ok.setEnabled(false));
                        }
                        target.addComponent(feedback);
                    }
                };
                link.add(entry);
                item.add(link);

            }
        });
        // ******************************************************************
        
        
    }

    @Override
    protected String getXinhaParameterName(XinhaImage k) {
        return k.getValue();
    }

    private boolean isValidName(String input) {
        if (input != null && !"".equals(input)) {
            return true;
        }
        error("Name is not allowed to be empty");
        return false;
    }

    @Override
    protected void onDetach() {
        this.nodeModel.detach();
        super.onDetach();
    }

    
    @Override
    protected void onOk() {
        if (uuid == null) {
            return;
        }
        
        String link = values.get(XinhaImage.URL);
        values.put(XinhaImage.URL, link);
         
        Node node = nodeModel.getNode();
        
        try {
            if(node.hasNode(link)) {
                return;
            }
            Node facetselect = node.addNode(link, HippoNodeType.NT_FACETSELECT);
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

    @Override
    protected String getSelectedValue() {
        String url = values.get(XinhaImage.URL);
        try {
            String imageUrl = "binaries" + nodeModel.getNode().getPath() + "/" + url;
            if(selectedRN != null && !selectedRN.equals("")) {
                imageUrl += "/"+url+"/"+selectedRN;
            }
            values.put(XinhaImage.URL, imageUrl);
            return super.getSelectedValue();
        } catch (RepositoryException e) {
            log.error("An error occured while trying to get image path", e);
            return "{}";
        }
    }

    private List<ImageItem> getImageItems(JcrNodeModel nodeModel2) {
        List<ImageItem> items = new ArrayList<ImageItem>();
        String gallerySearchPath = "/content/gallery-search";
        try {
            Session session = nodeModel.getNode().getSession();
            Node gallerySearchNode = (Node) session.getItem(gallerySearchPath);
            Node resultset = gallerySearchNode.getNode(HippoNodeType.HIPPO_RESULTSET);
            NodeIterator imageNodesIt = resultset.getNodes();
            while (imageNodesIt.hasNext()) {
                HippoNode imageNode = (HippoNode) imageNodesIt.nextNode();
                // nextNode can return null
                if (imageNode == null) {
                    continue;
                }
                Node canonical = imageNode.getCanonicalNode();
                if (canonical != null && canonical.getParent().isNodeType("mix:referenceable")) {
                    try {
                        canonical.getPrimaryItem().getName();

                        items.add(new ImageItem(canonical));
                    } catch (ItemNotFoundException e) {
                        log.error("gallery node does not have a primary item: skipping node: " + canonical.getPath());
                    }
                }
            }
        } catch (PathNotFoundException e) {
            log.error("Gallery Search node missing: " + gallerySearchPath);
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage(), e);
        }
        return items;
    }

    public static class ImageItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String path;
        private String uuid;
        private String nodeName;
        private String primaryItemName;
        private List<String> resourceDefinitions;

        public ImageItem(Node canonical) throws UnsupportedRepositoryOperationException, ItemNotFoundException, AccessDeniedException, RepositoryException {
            this.path = canonical.getPath();
            this.uuid = canonical.getParent().getUUID();
            this.primaryItemName = canonical.getPrimaryItem().getName();
            this.nodeName = canonical.getName();
            this.resourceDefinitions = new ArrayList<String>();
            NodeDefinition[] childDefs = canonical.getPrimaryNodeType().getChildNodeDefinitions();
            for (int i = 0; i < childDefs.length; i++) {
                if (!childDefs[i].getName().equals(primaryItemName) && childDefs[i].getDefaultPrimaryType() != null
                        && childDefs[i].getDefaultPrimaryType().isNodeType("hippo:resource")) {
                    resourceDefinitions.add(childDefs[i].getName());
                }
            }
        }

        public String getUuid() {
            return uuid;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getPrimaryUrl() {
            return "binaries" + path + "/" + primaryItemName;
        }

        public List<String> getResourceDefinitions(){
            return resourceDefinitions;
        }
        
        public String getNodeName() {
            return nodeName;
        }
    }

}
