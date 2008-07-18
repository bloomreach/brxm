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
package org.hippoecm.frontend.plugins.xinha.modal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.lookup.LookupTargetTreeView;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagePickerContentPanel extends XinhaContentPanel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(ImagePickerContentPanel.class);
    
    private static final long serialVersionUID = 1L;
    private JcrNodeModel nodeModel;
    private String uuid = null;
    private String altName = null;
    private String selectedimg = "";
    private String existsMsg = "";
    protected LookupTargetTreeView tree;
    
    public ImagePickerContentPanel(XinhaModalWindow modal, JcrNodeModel nodeModel, Map<String, String> parameters) {
        super(modal, parameters);
        
        ok.setEnabled(false);
        
        this.nodeModel = nodeModel;
        
        final TextField alt = new TextField("alt", new XinhaImageModel(parameters, XinhaImageProperty.ALT));
        form.add(alt);
        
        final Label exists = new Label("exists", new PropertyModel(this, "existsMsg"));
        exists.setOutputMarkupId(true);
        form.add(exists);
        
        OnChangeAjaxBehavior onChangeAjaxBehavior = new OnChangeAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                altName = alt.getInput();
                if(isValidName(altName) && uuid != null) {
                    target.addComponent(ok.setEnabled(true));
                    target.addComponent(exists);
                } else { 
                    target.addComponent(ok.setEnabled(false)); 
                    target.addComponent(exists);
                }     
            }
        };
        alt.add(onChangeAjaxBehavior);
        
        
        final Label selectedLabel = new Label("selectedimg"){
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
        
        final List<ImageItem> items = getImageItems(nodeModel);
            
        form.add(new ListView("item", items) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void populateItem(final ListItem item) {
                
                WebMarkupContainer entry = new WebMarkupContainer("entry");
                entry.add(new Label("thumbnail"){

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.put("src", ((ImageItem)item.getModelObject()).getUrl());
                        tag.put("width", "50");
                        super.onComponentTag(tag);
                    }
                    
                });
                
                final AjaxLink link = new AjaxLink("callback"){

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                         uuid = ((ImageItem)item.getModelObject()).getUuid();
                         selectedimg = ((ImageItem)item.getModelObject()).getUrl();
                         target.addComponent(selectedLabel);
                         
                         if(isValidName(altName) && uuid != null) {
                             target.addComponent(ok.setEnabled(true));
                             target.addComponent(exists);
                         } else { 
                             target.addComponent(ok.setEnabled(false)); 
                             target.addComponent(exists);
                         }   
                    }
                };
                link.add(entry);
                item.add(link);                

            }
        });
               
    }
    
    private boolean isValidName(String input) {
        if(input != null && !"".equals(input)) {
            try {
                if(nodeModel.getNode().hasNode(input)) {
                    existsMsg = "Name already exists in area. Choose other";
                    return false;
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            existsMsg = "";
            return true;
        } 
        existsMsg = "Name is not allowed to be empty";
        return false;
    }
    
    @Override
    protected void onSubmitCreateLinks() {
        if(uuid == null) {
            return;
        }
        
        String link = parameters.get(XinhaImageProperty.ALT.getValue());
        link = ISO9075Helper.encodeLocalName(link);
        parameters.put(XinhaImageProperty.ALT.getValue(), link);
        
        Node node = nodeModel.getNode();
        try {
            Node facetselect = node.addNode(link, HippoNodeType.NT_FACETSELECT);
            //todo fetch corresponding uuid of the chosen imageset
            facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
            facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
            facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
            facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
            // need a node save (the draft so no problem) to visualize images
            node.save();
        } catch (ItemExistsException e) {
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchNodeTypeException e) {
            e.printStackTrace();
        } catch (LockException e) {
            e.printStackTrace();
        } catch (VersionException e) {
            e.printStackTrace();
        } catch (ConstraintViolationException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected String getSelectedValue() {
        String alt = parameters.get(XinhaImageProperty.ALT.getValue());
        String absUrl = alt;
        try {
            absUrl = "binaries" + nodeModel.getNode().getPath() + "/" + alt;
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        parameters.put(XinhaImageProperty.URL.getValue(), absUrl);
        return MapToJavascriptObject(parameters);
    }

    private class XinhaImageModel implements IModel {
        private static final long serialVersionUID = 1L;

        private Map<String, String> values;
        private XinhaImageProperty p;

        public XinhaImageModel(Map<String, String> values, XinhaImageProperty p) {
            this.values = (values != null) ? values : new HashMap<String, String>();
            this.p = p;
        }

        public Object getObject() {
            return values.get(p.getValue());
        }

        public void setObject(Object object) {
            if (object == null)
                return;
            values.put(p.getValue(), (String) object);
        }

        public void detach() {
        }

    }
    
    private List<ImageItem> getImageItems(JcrNodeModel nodeModel2) {
        List<ImageItem> items = new ArrayList<ImageItem>();
        String gallerySearchPath = "/content/gallery-search";
        try {
            Session session = nodeModel.getNode().getSession();
            Node gallerySearchNode = (Node)session.getItem(gallerySearchPath);
            Node resultset = gallerySearchNode.getNode(HippoNodeType.HIPPO_RESULTSET);
            NodeIterator imageNodesIt = resultset.getNodes();
            while(imageNodesIt.hasNext()) {
                HippoNode imageNode = (HippoNode)imageNodesIt.nextNode();
                // nextNode can return null
                if(imageNode == null) {
                    continue;
                }
                Node canonical = imageNode.getCanonicalNode();
                if(canonical != null && canonical.getParent().isNodeType("mix:referenceable")) {
                    try {
                        canonical.getPrimaryItem().getName();
                        
                        items.add(new ImageItem(canonical.getParent().getUUID(), "binaries" + canonical.getPath() + "/" + canonical.getPrimaryItem().getName()));
                    }
                    catch (ItemNotFoundException e) {
                        log.error("gallery node does not have a primary item: skipping node: " + canonical.getPath());
                    }
                }
            }
        } 
        catch (PathNotFoundException e) {
            log.error("Gallery Search node missing: " + gallerySearchPath);
        }
        catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage(), e);
        }
        return items;
    }


    public static class ImageItem implements IClusterable
    {
        private static final long serialVersionUID = 1L;
        
        public String url;
        public String uuid;

        public ImageItem(String uuid, String url)
        {
            this.url = url;
            this.uuid = uuid;
        }

        public String getUuid()
        {
            return uuid;
        }
        public void setUuid(String uuid)
        {
            this.uuid = uuid;
        }
        public String getUrl()
        {
            return url;
        }
        public void setUrl(String url)
        {
            this.url = url;
        }
    }


}
