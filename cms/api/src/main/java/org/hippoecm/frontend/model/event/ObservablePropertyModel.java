/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.PropertyModel;

/**
 * A property model that observes an observable target.
 */
public class ObservablePropertyModel<T> extends PropertyModel<T> implements IObservable {

    private static final long serialVersionUID = 1L;

    private Observable observable;
    
    public ObservablePropertyModel(IObservable target, String expression) {
        super(target, expression);
        this.observable = new Observable(target);
    }

    @Override
    public void detach() {
        observable.detach();
        super.detach();
    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        observable.setObservationContext(context);
    }

    public void startObservation() {
        observable.startObservation();
    }

    public void stopObservation() {
        observable.stopObservation();
    }

}
