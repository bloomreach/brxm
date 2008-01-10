/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.variants;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.content.DocumentVariant;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Plugin for showing the available variants of a document
 */
public class VariantsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    // wicket:id's
    private static final String NODE_NAME_LABEL = "nodename";
    private static final String VARIANTS_LIST = "variants";
    private static final String VARIANT_LABEL = "variant";
    private static final String VARIANT_LINK = "variantlink";

    // Default labels
    // TODO: needs i18m
    private static final String NODE_NONE = "no node selected..";

    private JcrNodeModel selectedVariantNodeModel = null;
    
    /**
     * The label displaying the node name
     */
    protected String nodeName = NODE_NONE;
    
    

    /**
     * The list containing the variants for a hippo:handle
     */
    protected List<DocumentVariant> variantsList = new ArrayList<DocumentVariant>();

    public VariantsPlugin(PluginDescriptor descriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(descriptor, model, parentPlugin);

        JcrNodeModel handleModel = findHandle(model);
        
        if (handleModel != null) {
            HippoNode node = handleModel.getNode();
            
            try {
                nodeName = node.getDisplayName();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }

            Document document = new Document(handleModel);
            variantsList.addAll(document.getVariants());
        }
        else {
            variantsList.clear();
        }
         

        ListView listView = new ListView(VARIANTS_LIST, variantsList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final DocumentVariant variant = (DocumentVariant) item.getModelObject();
                //item.add(new Label(VARIANT_LABEL, variant));

                AjaxLink link = new AjaxLink(VARIANT_LINK, variant) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        
                        Channel channel = getDescriptor().getIncoming();
                        if (channel != null) {
                            Request request = channel.createRequest("select", variant.getNodeModel()
                                    .getMapRepresentation());
                            channel.send(request);
                            request.getContext().apply(target);
                        }
                    }

                };
                
                String prefix = "";

                if (variant.getNodeModel().equals(selectedVariantNodeModel)) {
                    prefix = "-> ";
                }
                
                item.add(link);
                link.add(new Label(VARIANT_LABEL, prefix + variant.getLanguage() + " - " + variant.getState()));

            }
        };
        add(new Label(NODE_NAME_LABEL, new PropertyModel(this, "nodeName")));
        add(listView);
    }
    
    /**
     * Finds the first parent node that is a handle 
     * @param nodeModel
     * @return the Node of the matching parent node, or null
     */
    private JcrNodeModel findHandle(JcrNodeModel nodeModel) {
        
        JcrNodeModel result = nodeModel;
        
        if (result != null) {
            Node resultNode = result.getNode();
        
            try {
                if (!resultNode.getPrimaryNodeType().isNodeType(HippoNodeType.NT_HANDLE)){
                    result = findHandle(result.getParentModel());
                }
            } catch (RepositoryException e) {
                result = null;
            }
        
        }
        
        return result;
    }


    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getData());
            // ignore documents; we select those ourselves
            
            nodeName = "";
            selectedVariantNodeModel = null;

            try {
                if (nodeModel.getNode().getPrimaryNodeType().isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    // The selected node is a variant. Select it in the list of variants.
                    
                    selectedVariantNodeModel = nodeModel;
                }
            } catch (RepositoryException e1) {
                e1.printStackTrace();
            }            

            Node resultNode = nodeModel.getNode();
            
            try {
                if (resultNode.getPrimaryNodeType().isNodeType(HippoNodeType.NT_REQUEST)) {
                    // The selected node is a request on a document. Find which document it refers to and select that.
                    
                    if (resultNode.hasProperty("document")) {

                        javax.jcr.Session session = (javax.jcr.Session)(((UserSession)Session.get()).getJcrSession());
                        
                        String docUUID = resultNode.getProperty("document").getString();
                        
                        selectedVariantNodeModel = new JcrNodeModel(session.getNodeByUUID(docUUID));
                    }
                        
                }
            } catch (ValueFormatException e1) {
                // document UUID is not a string
            } catch (PathNotFoundException e1) {
                e1.printStackTrace();
            } catch (ItemNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (RepositoryException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            
            try {
                    JcrNodeModel handleModel = findHandle(nodeModel);

                    variantsList.clear();
                    setNodeModel(handleModel);

                    if (handleModel != null) {
                        
                        HippoNode node = handleModel.getNode();

                        Document document = new Document(handleModel);
                        variantsList.addAll(document.getVariants());

                        nodeName = node.getDisplayName();

                    }

                    notification.getContext().addRefresh(this);
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        super.receive(notification);
    }
}
