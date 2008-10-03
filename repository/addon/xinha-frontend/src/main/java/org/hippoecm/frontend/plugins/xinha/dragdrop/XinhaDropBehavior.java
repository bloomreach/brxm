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

package org.hippoecm.frontend.plugins.xinha.dragdrop;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiHeaderContributor;
import org.hippoecm.frontend.plugins.yui.dragdrop.DropBehavior;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XinhaDropBehavior extends DropBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(XinhaDropBehavior.class);

    public XinhaDropBehavior(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }
    
    enum Type {
        Image, Document;
    }
    
    @Override
    public void renderHead(IHeaderResponse response) {
        YuiHeaderContributor.forModule(XinhaNamespace.NS, "xinhadropmodel").renderHead(response);
        super.renderHead(response);
    }

    @Override
    public void onDrop(IModel model, Map<String, String[]> parameters, AjaxRequestTarget target) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            
            Type type = getType(nodeModel);
            if(type == null)
                return;
            
            if(type == Type.Image) //images are dragged with the nt_resource node, use parent instead.
                nodeModel = nodeModel.getParentModel();
            
            String activeElement = parameters.containsKey("activeElement") ? parameters.get("activeElement")[0] : "";
            boolean emptySelection = parameters.containsKey("emptySelection") ? Boolean.parseBoolean(parameters.get("emptySelection")[0]) : true;
            
            switch (type) {
            case Image:
                if(emptySelection) {
                    insertImage(nodeModel, target);
                    break;
                } else if(activeElement.equals("img")) {
                    updateImage(nodeModel, target);
                    break;
                } 
            case Document:
                if(!emptySelection) {
                    if(activeElement.equals("")) {
                        //Only text selected, create internal link
                        insertLink(nodeModel, target);
                        break;
                    } else if(activeElement.equals("img")) {
                        insertLink(nodeModel, target);
                        break;
                    }
                } else {
                    break;    
                }
            default:
                if(activeElement.equals("a")) {
                    updateLink(nodeModel, target);
                }
                break;
            }
        }
    }
    
    private Type getType(JcrNodeModel nodeModel) {
        HippoNode node = nodeModel.getNode();
        try {
            if (node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                //asset or image
                
                //The ImagePicker expects a node of type ImageSet but as this is not a 'generic' nodeType we assume
                //that the node dropped on Xinha is the thumbnail of an Imageset and calling parent
                //will give us the ImageSet node

                //assume it's an image
                String mimeType = node.getProperty("jcr:mimeType").getValue().getString();
                
                if(mimeType.startsWith("image/")) {
                    return Type.Image;
                } 
            } 
            return Type.Document;
        } catch (RepositoryException e) {
            log.error("An error occurred while handling the onDrop event with node["
                    + nodeModel.getItemModel().getPath() + "]", e);
        }
        return null;

    }

    abstract protected void insertImage(JcrNodeModel model, AjaxRequestTarget target);
    
    abstract protected void updateImage(JcrNodeModel model, AjaxRequestTarget target);

    abstract protected void insertLink(JcrNodeModel model, AjaxRequestTarget target);
    
    abstract protected void updateLink(JcrNodeModel model, AjaxRequestTarget target);

}
