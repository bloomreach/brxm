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
package org.hippoecm.frontend.model.ocm;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrObject implements IDetachable, IObservable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrObject.class);

    private JcrNodeModel nodeModel;
    private IObserver observer;
    private IObservationContext obContext;

    public JcrObject(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    protected Node getNode() {
        return nodeModel.getNode();
    }

    protected JcrNodeModel getNodeModel() {
        return nodeModel;
    }
    
    public void save() {
        if (nodeModel.getNode() != null) {
            try {
                nodeModel.getNode().save();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Node does not exist");
        }
    }

    public void detach() {
        if (nodeModel != null) {
            nodeModel.detach();
        }
    }

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    protected IObservationContext getObservationContext() {
        return obContext;
    }
    
    protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
    }
    
    public void startObservation() {
        obContext.registerObserver(observer = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return nodeModel;
            }

            public void onEvent(Iterator<? extends IEvent> event) {
                JcrObject.this.processEvents(obContext, event);
            }

        });
    }

    public void stopObservation() {
        obContext.unregisterObserver(observer);
    }

}
