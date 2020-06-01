/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.onehippo.forge.selection.frontend.model;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.Observer;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;

/**
 * Observable wrapper around a JcrPropertyValueModel
 */
public class ObservableValueModel<T extends Serializable> extends LoadableDetachableModel<T> implements IObservable {

    private final JcrPropertyValueModel<T> valueModel;
    private IObservationContext<? extends IObservable> context;
    private Observer<IObservable> observer;

    public ObservableValueModel(JcrPropertyValueModel<T> valueModel) {
        this.valueModel = valueModel;
    }

    public JcrPropertyValueModel<T> getValueModel() {
        return valueModel;
    }

    @Override
    protected T load() {
        return valueModel.getObject();
    }

    @Override
    public void setObservationContext(final IObservationContext<? extends IObservable> context) {
        this.context = context;
    }

    @Override
    public void startObservation() {
        observer = new Observer<IObservable>(valueModel.getJcrPropertymodel()) {
            @Override
            public void onEvent(final Iterator<? extends IEvent<IObservable>> events) {
                context.notifyObservers(new EventCollection(events));
            }
        };
        context.registerObserver(observer);
    }

    @Override
    public void stopObservation() {
        context.unregisterObserver(observer);
    }
}
