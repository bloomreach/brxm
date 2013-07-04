/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragDropSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.DropBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XinhaDropBehavior extends DropBehavior {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(XinhaDropBehavior.class);

    public XinhaDropBehavior(IPluginContext context, IPluginConfig config) {
        super(new DragDropSettings(YuiPluginHelper.getConfig(config)));
    }

    enum Type {
        Image, Document;
    }

    /**
     * List of elements names that are allowed to have a link inserted. Note that "" means no element is selected,
     * thus a link can be inserted as well.
     */
    private String[] insertDocumentElements = new String[] { "", "p", "img", "li" }; //TODO: Try all Xinha possibilities to complete this list.

    @Override
    public void addHeaderContribution(IYuiContext context) {
        super.addHeaderContribution(context);
        context.addModule(XinhaNamespace.NS, "xinhadropmodel");
    }

    @Override
    public void onDrop(IModel model, IRequestParameters parameters, AjaxRequestTarget target) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) model;

            Type type = getType(nodeModel);
            if (type == null) {
                return;
            }

            if (type == Type.Image) {
                // images are dragged with the nt_resource node, use parent instead.
                nodeModel = nodeModel.getParentModel();
            }

            final Set<String> parameterNames = parameters.getParameterNames();
            String activeElement = parameterNames.contains("activeElement") ? parameters.getParameterValue("activeElement").toString() : "";
            boolean emptySelection = parameterNames.contains("emptySelection") ? Boolean.parseBoolean(parameters.getParameterValue("emptySelection").toString()) : true;

            switch (type) {
            case Image:
                if (emptySelection) {
                    insertImage(nodeModel, target);
                    break;
                } else if (activeElement.equals("img")) {
                    updateImage(nodeModel, target);
                    break;
                }
            case Document:
                if (!emptySelection) {
                    for (String el : insertDocumentElements) {
                        if (activeElement.equals(el)) {
                            insertLink(nodeModel, target);
                            break;
                        }
                    } 
                } else {
                    break;
                }
            default:
                if (activeElement.equals("a")) {
                    updateLink(nodeModel, target);
                }
                break;
            }
        }
    }

    private Type getType(JcrNodeModel nodeModel) {
        Node node = nodeModel.getNode();
        try {
            if (node.isNodeType(HippoNodeType.NT_RESOURCE)) {
                //asset or image

                //The ImagePicker expects a node of type ImageSet but as this is not a 'generic' nodeType we assume
                //that the node dropped on Xinha is the thumbnail of an Imageset and calling parent
                //will give us the ImageSet node

                //assume it's an image
                String mimeType = node.getProperty("jcr:mimeType").getValue().getString();

                if (mimeType.startsWith("image/")) {
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
