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

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.content.DocumentVariant;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.cmsprototype.frontend.model.workflow.WorkflowRequest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for showing the available variants of a document
 */
public class VariantsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(VariantsPlugin.class);

    // wicket:id's
    private static final String NODE_NAME_LABEL = "nodename";
    private static final String VARIANTS_LIST = "variants";
    private static final String VARIANT_LABEL = "variant";
    private static final String VARIANT_LINK = "variantlink";

    // Default labels
    // TODO: needs i18m
    private static final String NODE_NONE = "no node selected..";

    private Document document;
    private DocumentVariant selectedVariant;
    
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

        populateView(model);

        ListView listView = new ListView(VARIANTS_LIST, variantsList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final DocumentVariant variant = (DocumentVariant) item.getModelObject();

                AjaxLink link = new AjaxLink(VARIANT_LINK, variant) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Channel channel = getDescriptor().getIncoming();
                        if (channel != null) {
                            Request request = channel.createRequest("select", variant.getNodeModel().getMapRepresentation());
                            channel.send(request);
                            request.getContext().apply(target);
                        }
                    }

                };
                
                String prefix = "";

                if (variant.equals(selectedVariant)) {
                    prefix = "-> ";
                }
                
                item.add(link);
                link.add(new Label(VARIANT_LABEL, prefix + variant.getLanguage() + " - " + variant.getState()));

            }
        };
        add(new Label(NODE_NAME_LABEL, new PropertyModel(this, "nodeName")));
        add(listView);
    }
    

    private void populateView(JcrNodeModel nodeModel) {
        
        // first check if the node is a request
        try {
            WorkflowRequest request = new WorkflowRequest(nodeModel);
            selectedVariant = request.getDocumentVariant();
            if (selectedVariant != null) {
                setDocument(selectedVariant.getDocument());
                return;
            }
        } catch (ModelWrapException e) {
            selectedVariant = null;
        }
         
        // then see if node is a document variant
        try {
            selectedVariant = new DocumentVariant(nodeModel);
            setDocument(selectedVariant.getDocument());
            return;
        } catch (ModelWrapException e) {
            selectedVariant = null;
            document = null;
        }

        // then see if node is a document handle
        try {
            setDocument(new Document(nodeModel));
            return;
        } catch (ModelWrapException e) {
            document = null;
        }
        
        // catch all for any other nodeModel
        try {
            setNodeModel(nodeModel);
            variantsList.clear();
            nodeName = nodeModel.getNode().getDisplayName();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            nodeName = NODE_NONE;
        }
        
    }
    
    private void setDocument(Document newDoc) {
        if (newDoc != null) {
            document = newDoc;
            setNodeModel(document.getNodeModel());
            variantsList.clear();
            variantsList.addAll(document.getVariants());
            nodeName = document.getName();
        }
        else {
            document = null;
        }
    }

    
    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            populateView(new JcrNodeModel(notification.getData()));
            notification.getContext().addRefresh(this);
        }
        super.receive(notification);
    }
}
