/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.Observable;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard attributes of a hipposysedit:templatetype node related to the state of the type.
 * Figures out what CSS classes and description should be used to represent the state.
 */
public class TypeStateAttributes implements IObservable, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TypeStateAttributes.class);

    private JcrNodeModel nodeModel;
    private Observable observable;
    private transient boolean loaded = false;

    private transient String cssClass;
    private transient String description;

    public TypeStateAttributes(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
        observable = new Observable(nodeModel);
    }

    public String getDescription() {
        load();
        return description;
    }

    public String getCssClass() {
        load();
        return cssClass;
    }

    public void detach() {
        loaded = false;

        description = null;
        cssClass = null;

        nodeModel.detach();
        observable.detach();
    }

    void load() {
        if (!loaded) {
            observable.setTarget(null);
            try {
                final Node node = nodeModel.getNode();
                if (node != null) {
                    loadAttributes(node);
                }
            } catch (RepositoryException e) {
                log.info("Unable to obtain type state properties", e);
            }
            loaded = true;
        }
    }

    private void loadAttributes(final Node node) throws RepositoryException {
        final TypeState state = TypeState.getState(node);
        final String stateName = state.name().toLowerCase();
        cssClass = "state-" + stateName;
        description = new ClassResourceModel(stateName, TypeStateAttributes.class).getObject();
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
