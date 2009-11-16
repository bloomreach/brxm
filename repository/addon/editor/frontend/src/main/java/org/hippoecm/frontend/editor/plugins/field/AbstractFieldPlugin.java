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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.model.AbstractProvider;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFieldPlugin<P extends IModel, C extends IModel> extends ListViewPlugin implements
        ITemplateFactory<C> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractFieldPlugin.class);

    public static final String FIELD = "field";
    public static final String TYPE = "type";

    protected String mode;
    protected AbstractProvider<C> provider;

    private FieldPluginHelper helper;
    private TemplateController<C> controller;

    protected AbstractFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        controller = new TemplateController<C>(context, config, this);
        helper = new FieldPluginHelper(context, config);

        mode = config.getString(ITemplateEngine.MODE);
        if (mode == null) {
            log.error("No edit mode specified");
        }
    }

    @Override
    protected String getItemId() {
        String serviceId = getPluginContext().getReference(this).getServiceId();
        return serviceId + ".item";
    }

    @Override
    protected void onDetach() {
        if (provider != null) {
            provider.detach();
        }
        super.onDetach();
    }

    protected void updateProvider() {
        IFieldDescriptor field = helper.getField();
        if (field != null) {
            ITemplateEngine engine = getTemplateEngine();
            if (engine != null) {
                P model = (P) getModel();
                try {
                    ITypeDescriptor subType = engine.getType(field.getType());
                    provider = newProvider(field, subType, model);
                    if (provider != null) {
                        controller.stop();
                        controller.start(provider);
                    }
                } catch (TemplateEngineException ex) {
                    log.warn("Unable to obtain type descriptor for " + model, ex);
                }
            } else {
                log.warn("No engine found to display new model");
            }
        } else {
            setVisible(false);
        }
    }

    protected abstract AbstractProvider<C> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type, P parentModel);

    protected boolean canRemoveItem() {
        IFieldDescriptor field = helper.getField();
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || (field == null) || !field.isMultiple()) {
            return false;
        }
        return true;
    }

    protected boolean canReorderItems() {
        IFieldDescriptor field = helper.getField();
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || field == null || !field.isMultiple() || !field.isOrdered()) {
            return false;
        }
        return true;
    }

    public void onAddItem(AjaxRequestTarget target) {
        provider.addNew();
    }

    public void onRemoveItem(C childModel, AjaxRequestTarget target) {
        provider.remove(childModel);
    }

    public void onMoveItemUp(C model, AjaxRequestTarget target) {
        provider.moveUp(model);
        // reorderings are not detected by the observation manager, so do a hard refresh
        updateProvider();
        redraw();
    }

    protected FieldPluginHelper getFieldHelper() {
        return helper;
    }

    protected TemplateController<C> getController() {
        return controller;
    }

    protected ITemplateEngine getTemplateEngine() {
        return getPluginContext()
                .getService(getPluginConfig().getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
    }

    protected C findModel(IRenderService renderer) {
        return controller.findModel(renderer);
    }

    public IClusterControl getTemplate(C model) throws TemplateEngineException {
        ITemplateEngine engine = getTemplateEngine();
        IFieldDescriptor field = helper.getField();
        IClusterConfig template = engine.getTemplate(engine.getType(field.getType()), mode);

        IPluginConfig parameters = new JavaPluginConfig(getPluginConfig().getPluginConfig("cluster.options"));
        parameters.put(ITemplateEngine.ENGINE, getPluginConfig().getString(ITemplateEngine.ENGINE));
        parameters.put(RenderService.WICKET_ID, getItemId());
        parameters.put(ITemplateEngine.MODE, mode);

        return getPluginContext().newCluster(template, parameters);
    }

}
