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
package org.hippoecm.frontend.sa.template.plugins.field;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.types.FieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.TypeDescriptor;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.model.AbstractProvider;
import org.hippoecm.frontend.sa.template.model.NodeTemplateProvider;
import org.hippoecm.frontend.service.IRenderService;

public class NodeFieldPlugin extends FieldPlugin<JcrNodeModel, JcrNodeModel> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public NodeFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String caption = config.getString("caption");
        add(new Label("name", caption));

        add(createAddLink());
    }

    @Override
    protected AbstractProvider<JcrNodeModel> newProvider(FieldDescriptor descriptor, TypeDescriptor type,
            JcrNodeModel nodeModel) {
        return new NodeTemplateProvider(descriptor, type, nodeModel.getItemModel());
    }

    @Override
    public void onModelChanged() {
        updateProvider();
        replace(createAddLink());
    }

    @Override
    protected void onAddRenderService(Item item, IRenderService renderer) {
        final JcrNodeModel model = (JcrNodeModel) item.getModel();

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemoveItem(model, target);
            }
        };
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || field == null || field.isMandatory()) {
            remove.setVisible(false);
        }
        item.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoveItemUp(model, target);
            }
        };
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || field == null || !field.isOrdered()) {
            upLink.setVisible(false);
        }
        if (item.getIndex() == 0) {
            upLink.setEnabled(false);
        }
        item.add(upLink);
    }

    protected Component createAddLink() {
        if (ITemplateEngine.EDIT_MODE.equals(mode) && (field != null) &&  (field.isMultiple() || (provider.size() == 0))) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    NodeFieldPlugin.this.onAddItem(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }

}
