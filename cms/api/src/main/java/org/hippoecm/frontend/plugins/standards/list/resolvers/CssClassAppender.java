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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import java.util.Iterator;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;

public class CssClassAppender extends AttributeModifier implements IObservable {

    private static final long serialVersionUID = 1L;

    private Component component;
    private IObservationContext obContext;
    private IObserver observer;

    public CssClassAppender(IModel<String> model) {
        super("class", true, model);
    }

    @Override
    protected String newValue(final String currentValue, final String replacementValue) {
        if(currentValue == null) {
            if(replacementValue == null) {
               return "";
            }
            return replacementValue;
        } else if(replacementValue == null) {
            return currentValue;
        }
        return currentValue + " " + replacementValue;
    }

    @Override
    public void bind(Component hostComponent) {
        component = hostComponent;
    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.obContext = context;
    }

    public void startObservation() {
        if ((getReplaceModel() instanceof IObservable) && component.getOutputMarkupId()) {
            final IObservable observable = (IObservable) getReplaceModel();
            obContext.registerObserver(observer = new IObserver<IObservable>() {

                public IObservable getObservable() {
                    return observable;
                }

                public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                    AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null) {
                        target.add(component);
                    }
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
}
