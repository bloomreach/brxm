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
package org.hippoecm.frontend.model.event;

import org.apache.wicket.IClusterable;

/**
 * Interface implemented by observable objects.  When multiple different observables are
 * equivalent according to their {@link #equals(Object)} method, observation is started on
 * one instance.
 * <p>
 * This interface must be implemented by observable objects, but should not be invoked by
 * plugins.
 */
public interface IObservable extends IClusterable {

    /**
     * Before observation is started on the observable, an observation context is injected by
     * the observer registry.  This context can be used to notify listeners.
     */
    void setObservationContext(IObservationContext<? extends IObservable> context);
    
    /**
     * When the first {@link IObserver} of this observable is registered with the observer
     * registry, observation is started.  Implementations must notify observers until observation
     * is stopped.
     * <p>
     * An implementation should register listeners with external data sources, when appropriate.
     * It is possible for an observable to register as an observer for another observable.
     */
    void startObservation();

    /**
     * When the last {@link IObserver} unregisters, observation is stopped.  Any listeners
     * or observers registered by the observable must be unregistered by the implementation.
     */
    void stopObservation();

    /**
     * Equivalence of observables; observation will only be started on one instance.
     */
    boolean equals(Object obj);

    /**
     * @see #equals(Object)
     */
    int hashCode();

}
