/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.cache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * <p>
 *   Note that this class is <strong>not</strong> thread-safe : It should not be accessed by concurrent threads
 * </p>
 */
public class HstEventsCollector {

    private static final Logger log = LoggerFactory.getLogger(HstEventsCollector.class);

    private String rootPath;
    private Set<HstEvent> hstEvents = new HashSet<>();

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public synchronized boolean hasEvents() {
        return !hstEvents.isEmpty();
    }

    public synchronized void collect(EventIterator events) {
        while (events.hasNext()) {
            try {
                addEvent(events.nextEvent());
            } catch (RepositoryException e) {
                log.error("Exception while getting jcr event");
            }
        }
    }

    public synchronized void collect(final String... absEventPaths) {
        if (absEventPaths == null) {
            return;
        }
        for (String eventPath : absEventPaths) {
            addEvent(eventPath);
        }

    }

    public synchronized void clear() {
        hstEvents.clear();

    }

    public synchronized Set<HstEvent> getAndClearEvents() {
        if (hstEvents.isEmpty()) {
            return Collections.emptySet();
        }
        Set<HstEvent> events = Collections.unmodifiableSet(hstEvents);
        hstEvents = new HashSet<>();
        return events;

    }

    private void addEvent(final Event jcrEvent) throws RepositoryException {
        if (HippoNodeType.HIPPO_IGNORABLE.equals(jcrEvent.getUserData())) {
            log.debug("Ignore event '{}' because marked {}", jcrEvent.getPath(), HippoNodeType.HIPPO_IGNORABLE);
            return;
        }
        if (ignore(jcrEvent)) {
            log.debug("Ignore event '{}' because not an event below /hst:hst.", jcrEvent.getPath());
            return;
        }
        final HstEvent event;
        if (isPropertyEvent(jcrEvent)) {
            String nodePath = StringUtils.substringBeforeLast(jcrEvent.getPath(), "/");
            event = new HstEvent(nodePath, true);
            hstEvents.add(event);
        } else {
            event = new HstEvent(jcrEvent.getPath(), false);
            hstEvents.add(event);
        }
        log.debug("Collected event {}", event);
    }

    private void addEvent(final String nodePath) {
        if (ignore(nodePath)) {
            log.debug("Ignore event '{}' because not an event below /hst:hst.", nodePath);
            return;
        }
        hstEvents.add(new HstEvent(nodePath, false));
    }

    private boolean ignore(final Event jcrEvent) throws RepositoryException {
        return ignore(jcrEvent.getPath());
    }

    private boolean ignore(final String eventPath) {
        if (eventPath.startsWith(rootPath) && !eventPath.equals(rootPath)) {
            return false;
        }
        return true;
    }

    private boolean isPropertyEvent(final Event jcrEvent) {
        return jcrEvent.getType() == Event.PROPERTY_ADDED
                || jcrEvent.getType() == Event.PROPERTY_CHANGED
                || jcrEvent.getType() == Event.PROPERTY_REMOVED;
    }

}
