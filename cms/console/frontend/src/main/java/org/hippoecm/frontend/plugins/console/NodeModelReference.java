/*
 * Copyright 2011-2023 Bloomreach
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.console;

import javax.jcr.Node;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;

public class NodeModelReference implements IModelReference<Node> {

    private static final long serialVersionUID = 1L;
    private final Component component;
    private final IModel<Node> model;

    public NodeModelReference(Component component, final IModel<Node> model) {
        this.component = component;
        this.model = model;
    }

    @Override
    public IModel<Node> getModel() {
        return model;
    }

    @Override
    public void setModel(final IModel iModel) {
        component.setDefaultModel(iModel);
    }

    @Override
    public void detach() {
        // noop
    }

    @Override
    public void setObservationContext(final IObservationContext<? extends IObservable> context) {
        // noop
    }

    @Override
    public void startObservation() {
        // noop
    }

    @Override
    public void stopObservation() {
        // noop
    }
}
