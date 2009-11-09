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
package org.hippoecm.frontend.model;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.IPluginContext;

public class ModelReference<T> implements IModelReference<T> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private IObservationContext observationContext;
    private String id;
    private IModel<T> model;

    public ModelReference(String serviceId, IModel<T> model) {
        this.id = serviceId;
        this.model = model;
    }

    public void init(IPluginContext context) {
        this.context = context;
        context.registerService(this, id);
    }

    public void destroy() {
        context.unregisterService(this, id);
    }

    public IModel<T> getModel() {
        return model;
    }

    public void setModel(final IModel<T> newModel) {
        if (newModel != this.model && (newModel == null || !newModel.equals(this.model))) {
            final IModel<T> oldModel = this.model;
            this.model = newModel;
            if (observationContext == null) {
                return;
            }
            IEvent<IModelReference<T>> mce = new IModelChangeEvent<T>() {

                public IModel<T> getNewModel() {
                    return newModel;
                }

                public IModel<T> getOldModel() {
                    return oldModel;
                }

                public IModelReference<T> getSource() {
                    return ModelReference.this;
                }
            };
            EventCollection<IEvent<IModelReference<T>>> collection = new EventCollection<IEvent<IModelReference<T>>>();
            collection.add(mce);
            observationContext.notifyObservers(collection);
        }
    }

    public void setObservationContext(IObservationContext context) {
        this.observationContext = context;
    }

    public void startObservation() {
        // no listeners need to be registered
    }

    public void stopObservation() {
        // no listeners have been registered
    }

    public void detach() {
        if (model != null) {
            model.detach();
        }
    }

}
