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
package org.hippoecm.frontend.model.event;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrEvent implements IEvent<JcrNodeModel> {

    static final Logger log = LoggerFactory.getLogger(JcrEvent.class);

    private Event event;

    public JcrEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
    
    public JcrNodeModel getSource() {
        try {
            return new JcrNodeModel(event.getPath());
        } catch (RepositoryException ex) {
            log.error("unable to retrieve path from event", ex);
        }
        return null;
    }

}
