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

import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;

/**
 * An {@link IModel} that is observable by plugins.
 */
public class ObservableModel<T extends Serializable> extends Model<T> implements IObservable {

    private static final long serialVersionUID = 1L;

    private IObservationContext obContext;
    private boolean observing = false;

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

}
