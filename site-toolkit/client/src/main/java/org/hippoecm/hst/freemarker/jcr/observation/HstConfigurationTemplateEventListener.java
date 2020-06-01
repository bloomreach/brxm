/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.freemarker.jcr.observation;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstConfigurationTemplateEventListener extends RepositoryTemplateEventListener {

    private static final Logger log = LoggerFactory.getLogger(HstConfigurationTemplateEventListener.class);

    @Override
    public void onEvent(EventIterator events) {
        Set<String> pathsToRemoveFromCache = new HashSet<>();
        while (events.hasNext()) {
            final Event event = events.nextEvent();
            try {
                switch (event.getType()) {
                    case Event.PROPERTY_ADDED:
                        pathsToRemoveFromCache.add(getParentPath(event.getPath()));
                        break;
                    case Event.PROPERTY_CHANGED:
                        pathsToRemoveFromCache.add(getParentPath(event.getPath()));
                        break;
                    case Event.PROPERTY_REMOVED:
                        pathsToRemoveFromCache.add(getParentPath(event.getPath()));
                        break;
                    default:
                        pathsToRemoveFromCache.add(event.getPath());
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException during template change listener ", e);
            }
        }
        removeFromCache(pathsToRemoveFromCache);
    }

    private String getParentPath(final String propertyPath) {
        return propertyPath.substring(0, propertyPath.lastIndexOf("/"));
    }

}
