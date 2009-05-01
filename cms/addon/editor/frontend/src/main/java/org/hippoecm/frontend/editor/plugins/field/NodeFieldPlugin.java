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
package org.hippoecm.frontend.editor.plugins.field;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.model.AbstractProvider;
import org.hippoecm.frontend.editor.model.NodeTemplateProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFieldPlugin extends FieldPlugin<JcrNodeModel, JcrNodeModel> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(NodeFieldPlugin.class);

    private static final long serialVersionUID = 1L;

    public NodeFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        updateProvider();

        // use caption for backwards compatibility; i18n should use field name
        String captionKey = field != null ? field.getName() : config.getString("caption");
        add(new Label("name", new StringResourceModel(captionKey, this, null, config.getString("caption"))));

        Label required = new Label("required", "*");
        if (field != null && !field.isMandatory()) {
            required.setVisible(false);
        }
        add(required);

        add(createAddLink());
    }

    @Override
    protected AbstractProvider<JcrNodeModel> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            JcrNodeModel nodeModel) {
        try {
            JcrNodeModel prototype = (JcrNodeModel) getTemplateEngine().getPrototype(type);
            NodeTemplateProvider provider = new NodeTemplateProvider(descriptor, prototype, nodeModel.getItemModel());
            if (ITemplateEngine.EDIT_MODE.equals(mode) && !descriptor.isMultiple() && provider.size() == 0) {
                provider.addNew();
            }
            return provider;
        } catch (TemplateEngineException ex) {
            log.warn("Could not find prototype", ex);
            return null;
        }
    }

    @Override
    public void onModelChanged() {
        updateProvider();
        replace(createAddLink());
        redraw();
    }

    @Override
    protected void onAddRenderService(Item item, IRenderService renderer) {
        final JcrNodeModel model = findModel(renderer);

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemoveItem(model, target);
            }
        };
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || field == null || field.isMandatory() || !field.isMultiple()) {
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
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || field == null || !field.isMultiple() || !field.isOrdered()) {
            upLink.setVisible(false);
        } else if (model != null) {
            Node node = model.getNode();
            if (node != null) {
                try {
                    int index = node.getIndex();
                    if (index == 1) {
                        upLink.setVisible(false);
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        }

        if (item.getIndex() == 0) {
            upLink.setEnabled(false);
        }
        item.add(upLink);
    }

    protected Component createAddLink() {
        if (ITemplateEngine.EDIT_MODE.equals(mode) && (field != null) && field.isMultiple()) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    NodeFieldPlugin.this.onAddItem(target);
                }
            };
        } else {
            return new Label("add").setVisible(false);
        }
    }

}
