/*
 *  Copyright 2014-2021 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class WebFilesTemplateEventListener extends RepositoryTemplateEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebFilesTemplateEventListener.class);

    public void onEvent(EventIterator events) {
        Set<String> pathsToRemoveFromCache = new HashSet<>();
        while (events.hasNext()) {
            final Event event = events.nextEvent();
            try {
                final String eventPath = event.getPath();
                if (!eventPath.contains(".ftl")) {
                    log.debug("Event path '{}' is not for a freemarker template file, continue", eventPath);
                    continue;
                }
                final String nodePath;
                switch (event.getType()) {
                    case Event.PROPERTY_ADDED:
                        nodePath = getParentPath(eventPath);
                        break;
                    case Event.PROPERTY_CHANGED:
                        nodePath = getParentPath(eventPath);
                        break;
                    case Event.PROPERTY_REMOVED:
                        nodePath = getParentPath(eventPath);
                        break;
                    default:
                        nodePath = eventPath;
                }
                String freeMarkerFilePath = nodePath;
                while (!freeMarkerFilePath.endsWith(".ftl")) {
                    if (isEmpty(freeMarkerFilePath)) {
                        break;
                    }
                    freeMarkerFilePath = getParentPath(freeMarkerFilePath);
                }
                if (!isEmpty(freeMarkerFilePath)) {
                    pathsToRemoveFromCache.add(freeMarkerFilePath);
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException during template change listener ", e);
            }
        }
        removeFromCache(pathsToRemoveFromCache);
    }

    private String getParentPath(final String propertyPath) {
        final int slashIndex = propertyPath.lastIndexOf("/");
        if (slashIndex == -1) {
            return EMPTY;
        }
        return propertyPath.substring(0, slashIndex);
    }
}
