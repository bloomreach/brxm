/*
 *  Copyright 2010 Hippo.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.compare.Comparer;
import org.hippoecm.frontend.model.AbstractProvider;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceContext;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComparingController<C extends IModel> implements IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    class ItemEntry extends RenderService {
        private static final long serialVersionUID = 1L;

        C oldModel;
        FieldItem<C> oldFir;

        C newModel;
        FieldItem<C> newFir;

        IClusterControl cmpTpl;

        ItemEntry(ServiceContext sc, C model) throws TemplateEngineException {
            super(sc, itemConfig);

            oldModel = model;

            cmpTpl = factory.newTemplate(itemConfig.getString("cmp"), IEditor.Mode.VIEW);
            addExtensionPoint("cmp");

            String oldModelId = cmpTpl.getClusterConfig().getString(RenderService.MODEL_ID);
            ModelReference oldModelRef = new ModelReference(oldModelId, model);
            oldModelRef.init(sc);

            cmpTpl.start();
        }

        ItemEntry(ServiceContext sc, C oldModel, C newModel) throws TemplateEngineException {
            super(sc, itemConfig);

            this.oldModel = oldModel;
            this.newModel = newModel;

            cmpTpl = factory.newTemplate(itemConfig.getString("cmp"), IEditor.Mode.COMPARE);
            if (cmpTpl.getClusterConfig().getReferences().contains("model.compareTo")) {
                addExtensionPoint("cmp");

                String oldModelId = cmpTpl.getClusterConfig().getString("model.compareTo");
                ModelReference oldModelRef = new ModelReference(oldModelId, oldModel);
                oldModelRef.init(sc);

                String newModelId = cmpTpl.getClusterConfig().getString(RenderService.MODEL_ID);
                ModelReference newModelRef = new ModelReference(newModelId, newModel);
                newModelRef.init(sc);

                cmpTpl.start();

            } else {
                cmpTpl = null;

                addExtensionPoint("old");
                addExtensionPoint("new");

                if (oldModel != null) {
                    IClusterControl template = factory.newTemplate(itemConfig.getString("old"), IEditor.Mode.VIEW);
                    oldFir = new FieldItem<C>(sc, oldModel, null, template, null);
                }
                if (newModel != null) {
                    IClusterControl template = factory.newTemplate(itemConfig.getString("new"), IEditor.Mode.VIEW);
                    newFir = new FieldItem<C>(sc, newModel, null, template, null);
                }
            }
        }

        @Override
        public String getVariation() {
            if (oldFir != null || newFir != null) {
                return "compat";
            }
            return super.getVariation();
        }

        void destroy() {
            if (oldFir != null) {
                oldFir.destroy();
            }
            if (newFir != null) {
                newFir.destroy();
            }
            if (cmpTpl != null) {
                cmpTpl.stop();
            }
            ((ServiceContext) getPluginContext()).stop();
        }

        @Override
        public void onDetach() {
            if (oldFir != null) {
                oldFir.detach();
            }
            if (newFir != null) {
                newFir.detach();
            }
            super.onDetach();
        }
    }

    private IPluginContext context;
    @SuppressWarnings("unused")
    private IPluginConfig config;
    private ITemplateFactory<C> factory;
    private Set<ItemEntry> childTemplates;
    private JavaPluginConfig itemConfig;
    private Comparer comparer;

    public ComparingController(IPluginContext context, IPluginConfig config, ITemplateFactory<C> factory,
            Comparer comparer, String itemId) {
        this.context = context;
        this.config = config;
        this.factory = factory;
        this.comparer = comparer;

        this.itemConfig = new JavaPluginConfig();
        itemConfig.put("wicket.id", itemId);
        itemConfig.put("cmp", itemId + ".cmp");
        itemConfig.put("old", itemId + ".old");
        itemConfig.put("new", itemId + ".new");

        childTemplates = new HashSet<ItemEntry>();
    }

    public void start(AbstractProvider<C> oldProvider, AbstractProvider<C> newProvider, ITypeDescriptor type) {
        boolean identical = false;
        if (oldProvider.size() == newProvider.size()) {
            identical = true;
            Iterator<C> oldIter = oldProvider.iterator(0, oldProvider.size());
            Iterator<C> newIter = newProvider.iterator(0, newProvider.size());
            while (oldIter.hasNext()) {
                C oldModel = oldIter.next();
                if (!newIter.hasNext()) {
                    identical = false;
                    break;
                }
                C newModel = newIter.next();
                if (!comparer.areEqual(oldModel, newModel)) {
                    identical = false;
                    break;
                }
            }
            if (newIter.hasNext()) {
                identical = false;
            }
        }
        if (!identical) {
            Iterator<C> oldIter = oldProvider.iterator(0, oldProvider.size());
            Iterator<C> newIter = newProvider.iterator(0, newProvider.size());
            while (oldIter.hasNext()) {
                C model = oldIter.next();
                if (newIter.hasNext()) {
                    addModelComparison(model, newIter.next());
                } else {
                    addModelComparison(model, null);
                }
            }
            while (newIter.hasNext()) {
                addModelComparison(null, newIter.next());
            }
        } else {
            Iterator<C> oldIter = oldProvider.iterator(0, oldProvider.size());
            while (oldIter.hasNext()) {
                addModelView(oldIter.next());
            }
        }
    }

    public void stop() {
        for (ItemEntry entry : childTemplates) {
            entry.destroy();
        }
        childTemplates.clear();
    }

    public ItemEntry getFieldItem(IRenderService renderer) {
        return (ItemEntry) renderer;
    }

    private void addModelView(C model) {
        try {
            ServiceContext serviceContext = new ServiceContext(context);
            childTemplates.add(new ItemEntry(serviceContext, model));
        } catch (TemplateEngineException ex) {
            log.error("Failed to open editor for new model", ex);
        }
    }

    private void addModelComparison(C oldModel, C newModel) {
        try {
            ServiceContext serviceContext = new ServiceContext(context);
            childTemplates.add(new ItemEntry(serviceContext, oldModel, newModel));
        } catch (TemplateEngineException ex) {
            log.error("Failed to open editor for new model", ex);
        }
    }

    public void detach() {
        for (ItemEntry entry : childTemplates) {
            entry.detach();
        }
    }

}
