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
package org.hippoecm.frontend.plugins.cms.browse.gallery;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.NodeCell;
import org.hippoecm.repository.api.HippoNodeType;

public class ImageGalleryListingNodeCell extends NodeCell {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ImageGalleryPlugin plugin;
    private JcrNodeModel model;

    public ImageGalleryListingNodeCell(String id, JcrNodeModel model, String nodePropertyName, ImageGalleryPlugin plugin) {
        super(id, model, nodePropertyName);
        this.plugin = plugin;
        this.model = model;
    }
 
    @Override
    protected void onSelect(JcrNodeModel model, AjaxRequestTarget target) {
        plugin.onSelect(model, target);
    }

    @Override
    protected void addDefaultCustomizedLabel(JcrNodeModel model, String nodePropertyName, AjaxLink link) {
        
        if("primaryitem".equals(nodePropertyName)) {
           link.add(new Label("label", ""){

            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                String s = getUrl();
                if(s != null ) { 
                   s = "<img src=\"" + s + "\"/>" ;
                   replaceComponentTagBody(markupStream, openTag, s);
               } else {
                   super.onComponentTagBody(markupStream, openTag);
               }
            }
               
           });
        }
    }
    

    public String getUrl(){
        Node node = model.getNode();
        try {
            if(node.isNodeType(HippoNodeType.NT_HANDLE) &&  node.getParent().isNodeType("hippostd:gallery")) {
                if(node.hasNode(node.getName())) {
                    Node imageSet = node.getNode(node.getName());
                    try {
                       Item primItem = imageSet.getPrimaryItem();
                       if(primItem.isNode()) {
                          if(((Node)primItem).isNodeType(HippoNodeType.NT_RESOURCE)){
                          // node location of image
                          return "/binaries"+primItem.getPath();
                          } else {
                              ImageGalleryPlugin.log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                          }
                       } else {
                           ImageGalleryPlugin.log.warn("primary item must be a node for image set");
                       }
                    } catch (ItemNotFoundException e) {
                        ImageGalleryPlugin.log.warn("no primary item present for imageset");
                    }
                }
            } 
            else {
                ImageGalleryPlugin.log.debug("Cannot display image");
            }
        } catch (RepositoryException e) {
            ImageGalleryPlugin.log.error(e.getMessage());
        }
        return null;
    }
    
    @Override
    protected boolean hasDefaultCustomizedLabels(String nodePropertyName) {
        if("primaryitem".equals(nodePropertyName)) {
            return true;
        }
        return super.hasDefaultCustomizedLabels(nodePropertyName);
    }
    
}
