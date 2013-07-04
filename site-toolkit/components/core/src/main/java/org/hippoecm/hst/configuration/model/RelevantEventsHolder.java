/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility class to process jcr events in a from jcr detached hst events. This class
 * is targeted for keeping track of which configuration nodes are deleted, modified, or added. It also collapses events
 * that result in the same behavior: For example, an event for a node remove at /foo/bar/lux is not added if there is
 * already a node remove event for /foo/bar. 
 * 
 */
public class RelevantEventsHolder {

    private static final Logger log = LoggerFactory.getLogger(RelevantEventsHolder.class);

    final private String rootPath;
    final private String configPath;
    final private  String commonCatalogPath;
    final private String hostsPath;
    final private String blueprintsPath;
    final private String channelsPath;


    enum EventFor {
        ROOT_CONFIGURATION,
        CATALOG,
        HOSTS,
        BLUEPRINTS,
        CHANNELS,
        SITE
    }

    private Set<RelevantEvent> configurationEvents = new HashSet<RelevantEvent>();

    public RelevantEventsHolder(String rootPath) {
        this.rootPath = rootPath;
        configPath = rootPath+"/hst:configurations";
        commonCatalogPath = configPath+"/hst:catalog";
        hostsPath = rootPath+"/hst:hosts";
        blueprintsPath = rootPath+"/hst:blueprints";
        channelsPath = rootPath+"/hst:channels";
    }

    public void clear() {
        configurationEvents.clear();
    }

    public void addEvent(final Event jcrEvent) throws RepositoryException {
        if (!eventForHstConfig(jcrEvent)) {
            return;
        }
        RelevantEvent event = new RelevantEvent(jcrEvent);
        boolean added = configurationEvents.add(event);
        if (added) {
            log.debug("Added event '{}'", event.relevantPath);
        }
    }

    public void addEvent(final String eventPath) {
        if (!eventForHstConfig(eventPath)) {
            return;
        }
        RelevantEvent event = new RelevantEvent(eventPath);
        boolean added = configurationEvents.add(event);
        if (added) {
            log.debug("Added event '{}'", event.relevantPath);
        }
    }


    private boolean eventForHstConfig(final Event jcrEvent) throws RepositoryException {
        return eventForHstConfig(jcrEvent.getPath());
    }

    private boolean eventForHstConfig(final String eventPath) {
        if (eventPath.startsWith(rootPath) && !eventPath.equals(rootPath)) {
            return true;
        }
        log.debug("Found non hst config event path '{}'.", eventPath);
        return false;
    }

    public boolean hasEvents() {
        return !configurationEvents.isEmpty();
    }

    public boolean hasHstRootConfigurationEvents() {
        return hasEventsOfType(EventFor.ROOT_CONFIGURATION);
    }

    public Iterator<String> getHstRootConfigurationPathEvents() {
        return getEventsForOfType(EventFor.ROOT_CONFIGURATION);
    }

    public boolean hasSiteEvents() {
        return hasEventsOfType(EventFor.SITE);
    }

    public Iterator<String> getRootSitePathEvents() {
        return getEventsForOfType(EventFor.SITE);
    }

    public boolean hasCommonCatalogEvents() {
        return hasEventsOfType(EventFor.CATALOG);
    }

    public Iterator<String> getCommonCatalogPathEvents() {
        return getEventsForOfType(EventFor.CATALOG);
    }

    public boolean hasHostEvents() {
        return hasEventsOfType(EventFor.HOSTS);
    }

    public Iterator<String> getHostPathEvents() {
        return getEventsForOfType(EventFor.HOSTS);
    }

    public boolean hasBlueprintEvents() {
        return hasEventsOfType(EventFor.BLUEPRINTS);
    }

    public Iterator<String> getBlueprintPathEvents() {
        return getEventsForOfType(EventFor.BLUEPRINTS);
    }

    public boolean hasChannelEvents() {
        return hasEventsOfType(EventFor.CHANNELS);
    }

    public Iterator<String> getChannelPathEvents() {
        return getEventsForOfType(EventFor.CHANNELS);
    }

    private boolean hasEventsOfType(final EventFor eventFor) {
        for (RelevantEvent event : configurationEvents) {
            if (event.eventFor == eventFor) {
                return true;
            }
        }
        return false;
    }

    private Iterator<String> getEventsForOfType(final EventFor eventFor) {
        List<String> eventsForOfType = new ArrayList<String>();
        for (RelevantEvent event : configurationEvents) {
            if (event.eventFor == eventFor) {
                eventsForOfType.add(event.relevantPath);
            }
        }
        return eventsForOfType.iterator();
    }

    private boolean isPropertyEvent(final Event jcrEvent) {
        return jcrEvent.getType() == Event.PROPERTY_ADDED
                || jcrEvent.getType() == Event.PROPERTY_CHANGED
                || jcrEvent.getType() == Event.PROPERTY_REMOVED;
    }

    private class RelevantEvent {
      
        private String relevantPath;
        private EventFor eventFor;

        RelevantEvent(final String eventPath) {
            eventFor = getEventFor(eventPath);
            relevantPath = getRelevantPathOfEventFor(eventFor, eventPath);

        }

        RelevantEvent(final Event jcrEvent) throws RepositoryException {
            String nodePath;
            if (isPropertyEvent(jcrEvent)) {
                String eventPath = jcrEvent.getPath();
                nodePath = StringUtils.substringBeforeLast(eventPath, "/");
            } else {
                nodePath = jcrEvent.getPath();
            }
            eventFor = getEventFor(nodePath);
            relevantPath = getRelevantPathOfEventFor(eventFor, nodePath);
        }

        private String getRelevantPathOfEventFor(final EventFor eventFor, final String nodePath) {
            switch (eventFor) {
                case BLUEPRINTS :
                    // blueprints always get completely reloaded 
                    return blueprintsPath;
                case CHANNELS :
                    // channels always get completely reloaded 
                    return channelsPath;
                case HOSTS :
                    // hosts always get completely reloaded
                    return hostsPath;
                case CATALOG :
                    // commont catalog is completely reloaded, so just use commonCatalogPath
                    return commonCatalogPath;
                case SITE :
                    return getRelevantPathForSiteEvent(nodePath);
                case ROOT_CONFIGURATION:
                    return getRelevantPathForHstConfiguration(nodePath);
            }
            throw new IllegalStateException("Unknown EventFor '"+eventFor+"'");
        }

        private String getRelevantPathForHstConfiguration(final String nodePath) {
            if (nodePath.equals(configPath)) {
                return nodePath;
            }
            // for any change below a hst:configuration node including itself, we reload that entire 
            // hst:configuration node
            StringBuilder relevantHstConfigurationPath = new StringBuilder(configPath).append("/");
            String nodePathFromHstConfigurationNode =  nodePath.substring(configPath.length() + 1);
            String hstConfigurationNodeName = StringUtils.substringBefore(nodePathFromHstConfigurationNode, "/");
            relevantHstConfigurationPath.append(hstConfigurationNodeName);
            return relevantHstConfigurationPath.toString();
        }

        private String getRelevantPathForSiteEvent(final String nodePath) {
            // an event for a hst site is typically something like /hst:hst/hst:sites/myproject
            // however, it may also be at /hst:hst/hst:sites/myproject/hst:content : The latter hst:content we are not
            // interested in and can be removed (then the hst:site myproject will be reloaded)

            // below returns original if does not end with "/hst:content"
            return StringUtils.substringBefore(nodePath, "/"+HstNodeTypes.NODENAME_HST_CONTENTNODE);
           
        }

        private EventFor getEventFor(final String path) {
            if (path.startsWith(commonCatalogPath + "/") || path.equals(commonCatalogPath)) {
                return EventFor.CATALOG;
            } else if (path.startsWith(configPath + "/") || path.equals(configPath)) {
                return EventFor.ROOT_CONFIGURATION;
            } else if (path.startsWith(hostsPath + "/") || path.equals(hostsPath)) {
                return EventFor.HOSTS;
            } else if (path.startsWith(blueprintsPath + "/") || path.equals(blueprintsPath)) {
                return EventFor.BLUEPRINTS;
            } else if (path.startsWith(channelsPath + "/") || path.equals(channelsPath)) {
                return EventFor.CHANNELS;
            } else {
                // since the hst:sites node is a free name, we cannot check the path. If it
                // does not start with anything above, it must be an event for a hst:sites or hst:site node
                return EventFor.SITE;
            }
        }

        @Override
        public boolean equals(final Object obj) {
            if(obj instanceof RelevantEvent) {
                RelevantEvent compare = (RelevantEvent)obj;
                return relevantPath.equals(compare.relevantPath);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return relevantPath.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append(" { ");
            sb.append("nodePath=\"").append(relevantPath).append("\", ");
            sb.append(" }");
            return sb.toString();
        }
    }

}
