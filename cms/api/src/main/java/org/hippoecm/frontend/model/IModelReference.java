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

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;

/**
 * Service interface for sharing models.  When a model is shared between plugins and it is subject to change,
 * it should be wrapped in an IModelReference service.  The service is usually available under the configuration
 * key 'wicket.model'.
 * <p>
 * This service is observable as well, so it is possible to receive model change events by registering an
 * {@link IObserver} with the service as the observable.  An {@link IModelChangeEvent} should be sent whenever
 * {@link IModelReference#setModel} is invoked.
 * <p>
 * The default implementation {@link ModelReference}, takes care of sending the events.
 * <p>
 * Model services should be registered before any of the plugins that uses them is started.  This makes it
 * easier to create plugins that consume models (they don't need to register service trackers).  Since generally
 * a model is injected into a new plugin cluster, this is usually quite a natural thing to do.
 *
 * @param <T> the type of the object for the model
 */
public interface IModelReference<T> extends IDetachable, IObservable {
    final static String SVN_ID = "$Id$";

    /**
     * Event that's sent to {@link IObserver}s of the service.
     * When processing the event, the model service should not receive a new model.
     *
     * @param <T> the type of the object for the model
     */
    interface IModelChangeEvent<T> extends IEvent<IModelReference<T>> {

        /**
         * The previous model of the service.
         *
         * @return the old model
         */
        IModel<T> getOldModel();

        /**
         * The new model of the service.
         *
         * @return the new model
         */
        IModel<T> getNewModel();
    }

    /**
     * Retrieve the current model of the model service
     *
     * @return the model
     */
    IModel<T> getModel();

    /**
     * Update the model of the service.  Observers will be notified with an {@link IModelChangeEvent}.
     *
     * @param model the new model
     */
    void setModel(IModel<T> model);
}
