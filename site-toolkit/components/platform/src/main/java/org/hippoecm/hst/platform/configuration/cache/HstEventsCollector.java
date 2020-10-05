/*
 *  Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_DOMAINS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_UPSTREAM;

/*
 * <p>
 *   Note that this class is <strong>not</strong> thread-safe : It should not be accessed by concurrent threads
 * </p>
 */
public class HstEventsCollector {

    private static final Logger log = LoggerFactory.getLogger(HstEventsCollector.class);

    private final String rootPath;
    private Set<HstEvent> hstEvents = new HashSet<>();

    public HstEventsCollector(final String rootPath) {
        this.rootPath = rootPath;
    }

    public synchronized boolean hasEvents() {
        return !hstEvents.isEmpty();
    }

    public synchronized void collect(EventIterator events) {
        final Map<String, Set<Integer>> movedNodeDetectionMap = new HashMap<>();
        while (events.hasNext()) {
            try {
                addEvent(events.nextEvent(), movedNodeDetectionMap);
            } catch (RepositoryException e) {
                log.error("Exception while getting jcr event");
            }
        }
        for (String movedNodePath : movedNodeDetectionMap.keySet()) {
            if (movedNodeDetectionMap.get(movedNodePath).size() == 2) {
                hstEvents.add(new HstEvent(StringUtils.substringBeforeLast(movedNodePath, "/"), false));
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

    // meant for unit test only
    synchronized Set<HstEvent> getHstEvents() {
        return Collections.unmodifiableSet(hstEvents);
    }

    private void addEvent(final Event jcrEvent, final Map<String, Set<Integer>> movedNodeDetectionMap) throws RepositoryException {
        if (HippoNodeType.HIPPO_IGNORABLE.equals(jcrEvent.getUserData())) {
            log.debug("Ignore event '{}' because marked {}", jcrEvent.getPath(), HippoNodeType.HIPPO_IGNORABLE);
            return;
        }
        if (ignore(jcrEvent)) {
            log.debug("Ignore event '{}' because not an event below '{}'.", jcrEvent.getPath(), rootPath);
            return;
        }
        final String path = jcrEvent.getPath();
        final HstEvent event;
        if (isPropertyEvent(jcrEvent)) {
            event = new HstEvent(StringUtils.substringBeforeLast(path, "/"), true);
        } else {
            final int type = jcrEvent.getType();
            if (type == Event.NODE_REMOVED || type == Event.NODE_ADDED) {
                if (!movedNodeDetectionMap.containsKey(path)) {
                    movedNodeDetectionMap.put(path, new HashSet<Integer>());
                }
                movedNodeDetectionMap.get(path).add(type);
            }
            event = new HstEvent(path, false);
        }
        hstEvents.add(event);
        log.debug("Collected event {}", event);
    }

    private void addEvent(final String nodePath) {
        if (ignore(nodePath)) {
            log.debug("Ignore event '{}'", nodePath);
            return;
        }
        hstEvents.add(new HstEvent(nodePath, false));
    }

    private boolean ignore(final Event jcrEvent) throws RepositoryException {
        return ignore(jcrEvent.getPath());
    }

    private boolean ignore(final String eventPath) {
        if (eventPath.contains("/" + NODENAME_HST_UPSTREAM + "/") || eventPath.endsWith("/" + NODENAME_HST_UPSTREAM)) {
            return true;
        }

        if (!eventPath.startsWith(rootPath)) {
            log.warn("Unexpected monitored event outside the rootPath '{}'. Skip it", rootPath);
            return true;
        }

        if (eventPath.equals(rootPath)) {
            // ignore root path changes
            return true;
        }

        if (eventPath.startsWith(rootPath + "/" + NODENAME_HST_DOMAINS + "/") || eventPath.equals(rootPath + "/" + NODENAME_HST_DOMAINS)) {
            // ignore security domain configuration since not part of HST in memory model
            return true;
        }

        return false;
    }

    private boolean isPropertyEvent(final Event jcrEvent) {
        return jcrEvent.getType() == Event.PROPERTY_ADDED
                || jcrEvent.getType() == Event.PROPERTY_CHANGED
                || jcrEvent.getType() == Event.PROPERTY_REMOVED;
    }

}
