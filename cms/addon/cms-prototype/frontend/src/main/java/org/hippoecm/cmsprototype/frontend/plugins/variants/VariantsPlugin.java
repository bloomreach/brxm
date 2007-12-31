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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
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

        HippoNode node = model.getNode();
        try {
            nodeName = node.getDisplayName();
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                Document document = new Document(model);
                variantsList.addAll(document.getVariants());
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
                    	if(channel != null) {
                    		Request request = channel.createRequest("select", variant.getNodeModel().getMapRepresentation());
                    		channel.send(request);
                    		request.getContext().apply(target);
                    	}
                    }

                };
                item.add(link);
                link.add(new Label(VARIANT_LABEL, variant.getLanguage() + " - " + variant.getState()));

            }
        };
        add(new Label(NODE_NAME_LABEL, new PropertyModel(this, "nodeName")));
        add(listView);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getData());
            // ignore documents; we select those ourselves
            try {
                HippoNode node = nodeModel.getNode();
                if (!node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    setNodeModel(nodeModel);
                    variantsList.clear();

                    nodeName = node.getDisplayName();
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        Document document = new Document(nodeModel);
                        variantsList.addAll(document.getVariants());
                    }
	                notification.getContext().addRefresh(this);
	            }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        super.receive(notification);
    }
}
