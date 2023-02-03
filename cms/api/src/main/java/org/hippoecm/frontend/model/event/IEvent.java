/*
 *  Copyright 2008-2023 Bloomreach
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

/**
 * Event generated by an {@link IObservable}.  Implementations of {@link IObservable} are
 * encouraged to provide an IEvent subclass as part of their API.
 * <p>
 * Events are sent by observables by invoking {@link IObservationContext#notifyObservers(EventCollection)}.
 * They are received by observers in their {@link IObserver#onEvent(java.util.Iterator)} method.
 */
public interface IEvent<T extends IObservable> {

    /**
     * The {@link IObservable} that generated the event.
     */
    T getSource();

}
