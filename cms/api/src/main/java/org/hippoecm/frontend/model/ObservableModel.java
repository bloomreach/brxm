/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.IPluginContext;

/**
 * An {@link IModel} that is observable by plugins.
 */
public class ObservableModel<T extends Serializable> extends Model<T> implements IObservable {
    private static volatile AtomicInteger n_objects = new AtomicInteger();

    private IObservationContext obContext;
    private boolean observing = false;
    private final int objectId = n_objects.incrementAndGet();

    public ObservableModel(T object) {
        super(object);
    }

    @Override
    public void setObject(T object) {
        boolean wasObserving = observing;
        if (observing) {
            stopObservation();
        }
        super.setObject(object);
        if (wasObserving) {
            startObservation();
        }
        notifyObservers(new EventCollection());
    }

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
        observing = true;
    }

    public void stopObservation() {
        observing = false;
    }

    public void notifyObservers(EventCollection events) {
        if (observing) {
            obContext.notifyObservers(events);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return objectId;
    }

    /**
     * Retrieve the model object from the context with specific id. If it is not found,
     * create and register a new model instance.
     */
    public static <T extends Serializable> ObservableModel<T> from(final IPluginContext context, final String id) {
        ObservableModel<T> model = context.getService(id, ObservableModel.class);
        if (model == null) {
            model = new ObservableModel<>(null);
            context.registerService(model, id);
        }
        return model;
    }
}
