/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.container.session;

import java.util.EventObject;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * You can configure this listener in web.xml like the following example.
 * <pre>
 * &lt;listener&gt;
 *     &lt;listener-class&gt;org.hippoecm.hst.site.container.session.HttpSessionEventPublisher&lt;/listener-class&gt;
 * &lt;/listener&gt;
 * </pre>
 *
 * Publishes <code>HttpSessionApplicationEvent</code>s to the Component Manager.
 * Maps javax.servlet.http.HttpSessionListener.sessionCreated() to {@link HttpSessionCreatedEvent}.
 * Maps javax.servlet.http.HttpSessionListener.sessionDestroyed() to {@link HttpSessionDestroyedEvent}.
 */
public class HttpSessionEventPublisher implements HttpSessionListener {

    private static Logger log = LoggerFactory.getLogger(HttpSessionEventPublisher.class);

    public void sessionCreated(HttpSessionEvent event) {
        publishSessionEventToComponentManagers(event, true);
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        publishSessionEventToComponentManagers(event, false);
    }

    private void publishSessionEventToComponentManagers(HttpSessionEvent event, boolean isSessionCreatedEvent) {
        if (!HstServices.isAvailable()) {
            if (log.isDebugEnabled()) {
                log.debug("HST Services are not available yet. Skips publishing {} to componentManager(s).", event);
            }

            return;
        }

        EventObject eventObject = null;

        if (isSessionCreatedEvent) {
            eventObject = new HttpSessionCreatedEvent(event.getSession());
        } else {
            eventObject = new HttpSessionDestroyedEvent(event.getSession());
        }

        ComponentManager containerComponentManager = HstServices.getComponentManager();

        if (containerComponentManager == null) {
            log.warn("HST Services seem to be reloaded now. Skips session event publishing to componentManager(s): {}", eventObject);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Publishing event object to component manager(s): {}", eventObject);
        }

        containerComponentManager.publishEvent(eventObject);

        ComponentManager clientComponentManager = HstFilter.getClientComponentManager(event.getSession().getServletContext());

        if (clientComponentManager == null) {
            if (log.isDebugEnabled()) {
                log.debug("ClientComponentManager not found. Skips session created event publishing to clientComponentManager: {}", eventObject);
            }

            return;
        }

        clientComponentManager.publishEvent(eventObject);
    }
}