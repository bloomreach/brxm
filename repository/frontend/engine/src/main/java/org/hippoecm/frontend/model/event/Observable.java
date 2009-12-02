/*
 *  Copyright 2009 Hippo.
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

import java.util.Iterator;

import org.apache.wicket.model.IDetachable;

/**
 * Implementation of {@link IObservable} that monitors another observable.
 * It is intended to be used by clients to delegate the task of observation to.
 */
public class Observable implements IObservable, IDetachable {

    private IObservationContext obContext;
    private IObserver observer;

    private IObservable target;

    public Observable(IObservable target) {
        this.target = target;
    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.obContext = context;
    }

    public IObservable getTarget() {
        return target;
    }

    public void setTarget(IObservable target) {
        stopObservation();
        this.target = target;
        startObservation();
    }

    public void startObservation() {
        if (target != null && obContext != null) {
            obContext.registerObserver(observer = new IObserver<IObservable>() {

                public IObservable getObservable() {
                    return target;
                }

                public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                    obContext.notifyObservers(new EventCollection());
                }

            });
        }
    }

    public void stopObservation() {
        if (observer != null) {
            obContext.unregisterObserver(observer);
            observer = null;
        }
    }

    public void detach() {
        if (target instanceof IDetachable) {
            ((IDetachable) target).detach();
        }
    }

}
