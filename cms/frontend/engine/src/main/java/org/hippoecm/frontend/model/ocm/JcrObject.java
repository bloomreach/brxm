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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrObject implements IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrObject.class);

    private IPluginContext context;
    private JcrNodeModel nodeModel;
    private IObserver observer;

    public JcrObject(JcrNodeModel nodeModel, IPluginContext context) {
        this.nodeModel = nodeModel;
        this.context = context;
    }

    protected void init() {
        context.registerService(observer = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return JcrObject.this.nodeModel;
            }

            public void onEvent(IEvent event) {
                JcrObject.this.onEvent(event);
            }

        }, IObserver.class.getName());
    }

    protected void dispose() {
        context.unregisterService(observer, IObserver.class.getName());
    }

    protected IPluginContext getPluginContext() {
        return context;
    }
    
    protected Node getNode() {
        return nodeModel.getNode();
    }

    protected void onEvent(IEvent event) {
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

}
