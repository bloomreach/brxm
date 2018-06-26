/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observe the branchId.
 * <p>
 * To notify other observers of changes use @see(org.hippoecm.frontend.model.IModelReference#
 */
public class BranchIdModelObservable {

    private final Logger log = LoggerFactory.getLogger(BranchIdModelObservable.class);
    private final IPluginContext context;
    private final IPluginConfig config;
    private final Consumer<String> onEvent;
    private IModelReference<String> branchIdModelReference;

    public BranchIdModelObservable(final IPluginContext context, final IPluginConfig config, final Consumer<String> onEvent) {
        this.context = context;
        this.config = config;
        this.onEvent = onEvent;
    }

    public IModelReference<String> observeBranchId(final String initialBranchId) {
        final String editorId = getEditorId();

        branchIdModelReference = context.getService(editorId, IModelReference.class);
        if (branchIdModelReference == null) {
            branchIdModelReference = registerModelReference(initialBranchId);
        }
        context.registerService(new IObserver<IModelReference<String>>() {

            public IModelReference<String> getObservable() {
                return branchIdModelReference;
            }

            public void onEvent(Iterator<? extends IEvent<IModelReference<String>>> event) {
                IModel<String> branchIdModel = branchIdModelReference.getModel();
                String branchId = branchIdModel.getObject();
                onEvent.accept(branchId);
            }

        }, IObserver.class.getName());
        return branchIdModelReference;
    }

    public String getBranchId(){
        return branchIdModelReference.getModel().getObject();
    }

    public void setBranchId(String branchId){
        branchIdModelReference.getModel().setObject(branchId);
    }

    private ModelReference<String> registerModelReference(final String initialBranchId) {
        final String editorId = getEditorId();
        log.debug("No service found with id:{} of type{}", editorId, IModelReference.class.getName());
        log.debug("Creating model reference for a model containing the branchId ( initially '{}') ", initialBranchId);
        final ModelReference<String> modelReference = new ModelReference<>(editorId, new Model<>(initialBranchId));
        modelReference.init(context);
        return modelReference;
    }

    private String getEditorId() {
        return config.getString("editor.id");
    }


}
