/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.IPluginContext;

/**
 * The default implementation of the {@link IModelReference} service interface.
 * <p>
 * After a model service is created, it can be made to register it self under the provided service id
 * using the {@link ModelReference#init} method.
 * <p>
 * When model services are no longer needed, e.g. because the cluster that need it is no longer active,
 * be sure to unregister the service using {@link ModelReference#destroy}.
 *
 * @param <T> the type of the object for the shared model
 */
public class ModelReference<T> implements IModelReference<T> {

    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private IObservationContext observationContext;
    private String id;
    private IModel<T> model;

    /**
     * Construct a model service with a given service id and initial model.
     *
     * @param serviceId the service id that the service will be registered under
     * @param model the initial model
     */
    public ModelReference(String serviceId, IModel<T> model) {
        this.id = serviceId;
        this.model = model;
    }

    /**
     * Register the service with the specified service id.
     *
     * @param context the plugin context to use for registration
     */
    public void init(IPluginContext context) {
        this.context = context;
        context.registerService(this, id);
    }

    /**
     * Unregister the service with the specified service id.
     */
    public void destroy() {
        context.unregisterService(this, id);
    }

    @Override
    public IModel<T> getModel() {
        return model;
    }

    @Override
    public void setModel(final IModel<T> newModel) {
        if (newModel != this.model && (newModel == null || !newModel.equals(this.model))) {
            final IModel<T> oldModel = this.model;
            this.model = newModel;
            if (observationContext == null) {
                return;
            }
            IEvent<IModelReference<T>> mce = new IModelChangeEvent<T>() {

                @Override
                public IModel<T> getNewModel() {
                    return newModel;
                }

                @Override
                public IModel<T> getOldModel() {
                    return oldModel;
                }

                @Override
                public IModelReference<T> getSource() {
                    return ModelReference.this;
                }
            };
            EventCollection<IEvent<IModelReference<T>>> collection = new EventCollection<IEvent<IModelReference<T>>>();
            collection.add(mce);
            observationContext.notifyObservers(collection);
        }
    }

    @Override
    public void setObservationContext(IObservationContext context) {
        this.observationContext = context;
    }

    @Override
    public void startObservation() {
        // no listeners need to be registered
    }

    @Override
    public void stopObservation() {
        // no listeners have been registered
    }

    @Override
    public void detach() {
        if (model != null) {
            model.detach();
        }
    }

}
