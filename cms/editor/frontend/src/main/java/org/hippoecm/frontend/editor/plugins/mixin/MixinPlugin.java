/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginHelper;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that renders a list of configured mixin types in the document editor as checkboxes.
 */
public class MixinPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(MixinPlugin.class);

    protected Mode mode;
    protected FieldPluginHelper helper;

    private List<MixinModel> available;
    private Map<String, IClusterControl> controllers;

    public MixinPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        controllers = new HashMap<>();

        mode = Mode.fromString(config.getString(ITemplateEngine.MODE), Mode.VIEW);
        if (mode == Mode.COMPARE) {
            mode = Mode.VIEW;
        }
        helper = new FieldPluginHelper(context, config);

        final String defaultCaption = new StringResourceModel("mixins", this, null).getString();
        add(new Label("name", helper.getCaptionModel(this, defaultCaption)));

        ITemplateEngine engine = context.getService(config.getString(ITemplateEngine.ENGINE), ITemplateEngine.class);

        String[] mixins = config.getStringArray("mixins");
        available = new ArrayList<>(mixins.length);
        for (String mixin : mixins) {
            try {
                MixinModel model = new MixinModel(engine, mixin);
                available.add(model);
                if (model.getObject()) {
                    controllers.put(mixin, startMixin(mixin));
                }
            } catch (TemplateEngineException ex) {
                log.error("Unable to start editor for mixin " + mixin, ex);
            }
        }

        add(new RefreshingView("mixins") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<MixinModel> getItemModels() {
                return available.iterator();
            }

            @Override
            protected void populateItem(Item item) {
                final MixinModel model = (MixinModel) item.getModel();
                final AjaxCheckBox checkbox = new AjaxCheckBox("mixin", model) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean value = getModelObject();
                        String mixin = model.getMixin();
                        if (value) {
                            if (!controllers.containsKey(mixin)) {
                                try {
                                    controllers.put(mixin, startMixin(mixin));
                                } catch (TemplateEngineException ex) {
                                    log.error("Unable to start editor for mixin " + mixin, ex);
                                }
                            }
                        } else {
                            if (controllers.containsKey(mixin)) {
                                IClusterControl cluster = controllers.remove(mixin);
                                cluster.stop();
                            }
                        }
                        target.add(this);
                    }

                };
                checkbox.setOutputMarkupId(true);
                checkbox.setEnabled(mode == Mode.EDIT && !model.isPrimaryNodeType());
                item.add(checkbox);
                item.add(new Label("label", new TypeTranslator(new JcrNodeTypeModel(model.getMixin())).getTypeName()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onComponentTag(ComponentTag tag) {
                        tag.put("for", checkbox.getInputName());
                        super.onComponentTag(tag);
                    }
                });
            }

        });
    }

    protected IClusterControl startMixin(String mixin) throws TemplateEngineException {
        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();
        ITemplateEngine engine = context.getService(config.getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
        IClusterConfig template = engine.getTemplate(engine.getType(mixin), mode);
        IClusterControl control = getPluginContext().newCluster(template, config.getPluginConfig("cluster.options"));
        control.start();
        return control;
    }

    private static boolean hasMixin(Node node, String type) throws RepositoryException {
        if (!node.hasProperty("jcr:mixinTypes")) {
            return false;
        }
        NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
        for (Value value : node.getProperty("jcr:mixinTypes").getValues()) {
            NodeType nt = ntMgr.getNodeType(value.getString());
            if (nt.isNodeType(type)) {
                return true;
            }
        }
        return false;
    }

    private class MixinModel implements IModel<Boolean> {
        private static final long serialVersionUID = 1L;

        String name;
        String realType;

        MixinModel(ITemplateEngine engine, String mixin) throws TemplateEngineException {
            this.name = mixin;
            ITypeDescriptor type = engine.getType(mixin);
            this.realType = type.getType();
        }

        public String getMixin() {
            return name;
        }

        public Boolean getObject() {
            JcrNodeModel nodeModel = (JcrNodeModel) MixinPlugin.this.getDefaultModel();
            try {
                Node node = nodeModel.getNode();
                if (node != null) {
                    return hasMixin(node, realType);
                } else {
                    return false;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return false;
        }

        public void setObject(Boolean value) {
            JcrNodeModel nodeModel = (JcrNodeModel) MixinPlugin.this.getDefaultModel();
            try {
                Node node = nodeModel.getNode();
                if (value) {
                    if (!hasMixin(node, realType)) {
                        node.addMixin(realType);
                    }
                } else {
                    if (hasMixin(node, realType)) {
                        node.removeMixin(realType);
                    }
                }
                node.getSession().save();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        boolean isPrimaryNodeType() {
            JcrNodeModel nodeModel = (JcrNodeModel) MixinPlugin.this.getDefaultModel();
            try {
                Node node = nodeModel.getNode();
                if (node.getPrimaryNodeType().isNodeType(realType)) {
                    return true;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return false;
        }

        public void detach() {
        }
    }

}