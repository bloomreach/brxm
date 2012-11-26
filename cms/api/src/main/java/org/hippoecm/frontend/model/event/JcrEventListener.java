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
package org.hippoecm.frontend.model.event;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrEventListener implements EventListener, IClusterable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrEventListener.class);

    private IObservationContext<JcrNodeModel> context;
    private int eventTypes;
    private String absPath;
    private boolean isDeep;
    private String[] uuid;
    private String[] nodeTypeName;

    public JcrEventListener(IObservationContext<JcrNodeModel> context, int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName) {
        this.context = context;
        this.eventTypes = eventTypes;
        this.absPath = absPath;
        this.isDeep = isDeep;
        this.uuid = uuid;
        this.nodeTypeName = nodeTypeName;
    }

    public void onEvent(EventIterator events) {
        if (context != null) {
            EventCollection<IEvent<JcrNodeModel>> list = new EventCollection<IEvent<JcrNodeModel>>();
            while (events.hasNext()) {
                list.add(new JcrEvent(events.nextEvent()));
            }
            context.notifyObservers(list);
        } else {
            log.error("No observation context present");
        }
    }

    public void start() {
        UserSession session = UserSession.get();
        ObservationManager obMgr = session.getObservationManager();
        try {
            obMgr.addEventListener(this, eventTypes, absPath, isDeep, uuid, nodeTypeName, false);
        } catch (PathNotFoundException ex) {
            log.warn("Not starting event listener for non existing path: " + absPath);
        } catch (RepositoryException ex) {
            log.error("unable to register event listener", ex);
        }
    }

    public void stop() {
        UserSession session = UserSession.get();
        ObservationManager obMgr = session.getObservationManager();
        try {
            obMgr.removeEventListener(this);
        } catch (RepositoryException ex) {
            log.error("unable to unregister event listener, " + ex.getMessage());
        }
    }

    // re-register listener when it is deserialized

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        if (context != null) {
            start();
        }
    }
}
