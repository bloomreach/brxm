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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class VariantsPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    // wicket:id's
    private static final String NODE_NAME_LABEL = "nodename";
    private static final String VARIANTS_LIST = "variants";
    private static final String VARIANT_LABEL = "variant";

    // TODO: Replace with HippoNodeType when available: HREPTWO-342
    private static final String HIPPO_LANGUAGE = "language";
    private static final String HIPPO_STATE = "state";

    // Default labels
    // TODO: needs i18m
    private static final String NO_STATE = "no workflow";
    private static final String NO_LANGUAGE = "all languages";
    private static final String NODE_ERROR = "error";
    private static final String NODE_NONE = "no node selected..";

    /**
     * The label displaying the node name
     */
    protected String nodeName = NODE_NONE;

    /**
     * The list containing the variants for a hippo:handle
     */
    protected List<String> variantsList = new ArrayList<String>();

    /**
     * Plugin for showing the available variants of a document
     * @param descriptor PluginDescriptor
     * @param model JcrNodeModel
     * @param parentPlugin Plugin
     */
    public VariantsPlugin(PluginDescriptor descriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(descriptor, model, parentPlugin);

        ListView listView = new ListView(VARIANTS_LIST, variantsList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                String variant = (String) item.getModelObject();
                item.add(new Label(VARIANT_LABEL, variant));
            }
        };
        add(new Label(NODE_NAME_LABEL, new PropertyModel(this, "nodeName")));
        add(listView);
    }

    /**
     * Updater for the node name and the variants list
     * @param target AjaxRequestTarget
     *  @param event PluginEvent
     */
    public void update(AjaxRequestTarget target, PluginEvent event) {
        JcrNodeModel nodeModel = event.getNodeModel(JcrEvent.NEW_MODEL);
        if (nodeModel != null) {
            setNodeModel(nodeModel);
            HippoNode node = nodeModel.getNode();
            variantsList.clear();
            if (node != null) {
                try {
                    nodeName = node.getDisplayName();
                    if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        node = (HippoNode) node.getParent();
                    }
                    if (node != null && node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        updateVariants(node);
                    }
                } catch (RepositoryException e) {
                    nodeName = NODE_ERROR;
                }
            } else {
                nodeName = NODE_NONE;
            }
            
            if (target != null && findPage() != null) {
                target.addComponent(this);
            }
        }
    }

    /**
     * Build the variants list for a hippo:handle node
     * @param node Expects a hippo:handle node
     * @throws RepositoryException
     */
    protected void updateVariants(HippoNode node) throws RepositoryException {
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            StringBuffer variant = new StringBuffer();
            HippoNode docNode = (HippoNode) iter.next();
            if (docNode.hasProperty(HIPPO_LANGUAGE)) {
                String language = docNode.getProperty(HIPPO_LANGUAGE).getString();
                variant.append(language);
            } else {
                variant.append(NO_LANGUAGE);
            }
            variant.append(" - ");
            if (docNode.hasProperty(HIPPO_STATE)) {
                String state = docNode.getProperty(HIPPO_STATE).getString();
                variant.append(state);
            } else {
                variant.append(NO_STATE);
            }
            variantsList.add(variant.toString());
        }
    }
}
