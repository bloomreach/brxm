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
package org.hippoecm.frontend.editor.validator;

import java.util.Iterator;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.validation.IValidationResult;

/**
 * Filter validation results.  Useful to pass the results of a validation to
 * plugins that are unaware of the container model, but only operate on a field
 * value.
 */
public class FilteredValidationModel extends Model<IValidationResult> implements IObservable {
    private static final long serialVersionUID = 1L;

    private IObservationContext obContext;
    private IObserver observer;

    private IModel<IValidationResult> upstreamModel;

    public FilteredValidationModel(IModel<IValidationResult> upstreamModel, IFieldDescriptor field) {
        super(new FilteredValidationResult(upstreamModel.getObject(), field));
        this.upstreamModel = upstreamModel;
    }

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
        if (upstreamModel instanceof IObservable) {
            final IObservable upstream = (IObservable) upstreamModel;
            obContext.registerObserver(observer = new IObserver<IObservable>() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return upstream;
                }

                public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                    obContext.notifyObservers(new EventCollection(events));
                }

            });
        }
    }

    public void stopObservation() {
        obContext.unregisterObserver(observer);
        observer = null;
    }

}
