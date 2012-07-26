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
 * The context representing the observer registry to the observable.
 * Framework clients should not implement this interface.
 */
public interface IObservationContext<T extends IObservable> extends IClusterable {

    /**
     * Notify observers of events that pertain to an observable.
     * The events are dispatched to registered observers.
     * <p>
     * Implementations should minimize the number of calls to this method,
     * as all observers will be notified.
     * 
     * @param events The events to be dispatched to observers
     */
    void notifyObservers(EventCollection<IEvent<T>> events);

    /**
     * Register an observer.  Allows observables to delegate subscriptions to one another.
     * When an observable has been implemented in terms of another observable, it can
     * translate events of the underlying observable into events that are appropriate for
     * its own type.
     * 
     * @param observer
     */
    void registerObserver(IObserver<?> observer);

    /**
     * Unregister an observer.
     * 
     * @param observer
     */
    void unregisterObserver(IObserver<?> observer);

}
