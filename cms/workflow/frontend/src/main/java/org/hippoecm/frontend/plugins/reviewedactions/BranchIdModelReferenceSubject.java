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
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.addon.workflow.BranchIdObserver;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers and unregisters observers of a branchIdModelReference.
 *
 * This class does not destroy the {@link ModelReference} instance. When the user logs out it is destroyed.
 */
public class BranchIdModelReferenceSubject {

    public static final String UNDEFINED = "undefined";
    private final Logger log = LoggerFactory.getLogger(BranchIdModelReferenceSubject.class);
    private IModelReference<String> branchIdModelReference;
    private IObserver<IModelReference<String>> modelReferenceObserverService;

    /**
     * Registers the observer and creates a {@link ModelReference} if not yet present.
     *
     * @param observer
     */
    public void registerObserver(final BranchIdObserver observer) {
        final IPluginContext pluginContext = observer.getPluginContext();
        final String referenceModelIdentifier = getReferenceModelIdentifier(observer.getBranchIdModelReferenceIdentifier());
        branchIdModelReference = getOrCreateModelReference(pluginContext, referenceModelIdentifier);

        modelReferenceObserverService = createObserverService(observer);
        log.debug("Register observer with identifier:{}", referenceModelIdentifier);
        pluginContext.registerService(modelReferenceObserverService, IObserver.class.getName());
    }

    private String getReferenceModelIdentifier(final String handleIdentifier) {
        return getClass().getName() + "." + handleIdentifier + "." + getUserId();
    }

    private IObserver<IModelReference<String>> createObserverService(final BranchIdObserver observer) {
        return new IObserver<IModelReference<String>>() {

            public IModelReference<String> getObservable() {
                return branchIdModelReference;
            }

            public void onEvent(Iterator<? extends IEvent<IModelReference<String>>> event) {
                IModel<String> branchIdModel = branchIdModelReference.getModel();
                String branchId = branchIdModel.getObject();
                observer.onBranchIdChanged(branchId);
            }

        };
    }

    public void unregisterObserver(final BranchIdObserver observer) {
        log.debug("Unregister service:{}", modelReferenceObserverService);
        observer.getPluginContext().unregisterService(modelReferenceObserverService, IObserver.class.getName());
    }

    public String getBranchId() {
        return branchIdModelReference.getModel().getObject();
    }

    public void setBranchId(String branchId) {
        log.debug("Set branchId to:{} for user:{}", branchId, getUserId());
        branchIdModelReference.setModel(new Model<>(branchId));
    }

    public void setInitialBranchId(String branchId){
        if (getBranchId().equals(UNDEFINED)){
            setBranchId(branchId);
        }
    }

    private String getUserId() {
        return UserSession.get().getJcrSession().getUserID();
    }

    private IModelReference<String> getOrCreateModelReference(IPluginContext context, String identifier) {
        final Optional<IModelReference> service = Optional.ofNullable(context.getService(identifier, IModelReference.class));
        return service.orElse(createBranchIdModelReference(context, identifier));
    }

    private IModelReference<String> createBranchIdModelReference(final IPluginContext context, final String identifier) {
        log.debug("Creating model reference for a model containing the branchId for observer with id:{}", identifier);
        ModelReference<String> modelReference = new ModelReference<>(identifier, new Model<>(UNDEFINED));
        modelReference.init(context);
        return modelReference;
    }
}
