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
package org.hippoecm.frontend.model.event;

import java.util.EventListener;
import java.util.Iterator;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.IPluginContext;

/**
 * This interface defines the contract for a service that updates its internal state in
 * response to changes in an observable object (IObservable). Instances should be
 * registered as a service ({@link IPluginContext#registerService(IClusterable, String)})
 * with name IObserver.class.getName().  The observer registry will notify the observer
 * of any events sent by the observable.
 */
public interface IObserver<T extends IObservable> extends EventListener, IClusterable {

    /**
     * The observable that the observer is interested in.  This observable may not
     * change, w.r.t. the {@link IObservable#equals} method, while the observer is
     * registered.
     */
    T getObservable();

    /**
     * Callback that is invoked when the observable sends events.  The iterator is
     * guaranteed to be non-empty.
     */
    void onEvent(Iterator<? extends IEvent<T>> events);

}
