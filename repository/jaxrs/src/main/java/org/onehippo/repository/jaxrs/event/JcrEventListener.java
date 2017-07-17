/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.jaxrs.event;

import java.util.Arrays;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JcrEventListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(JcrEventListener.class);

    private final int eventTypes;
    private final String absPath;
    private final boolean isDeep;
    private final String[] uuid;
    private final String[] nodeTypeName;

    public JcrEventListener(
            final int eventTypes,
            final String absPath,
            final boolean isDeep,
            final String[] uuid,
            final String[] nodeTypeName
    ) {
        this.eventTypes = eventTypes;
        this.absPath = absPath;
        this.isDeep = isDeep;
        this.uuid = uuid;
        this.nodeTypeName = nodeTypeName;
    }

    public void attach(final ObservationManager manager) {
        try {
            manager.addEventListener(this, eventTypes, absPath, isDeep, uuid, nodeTypeName, false);
        } catch (final PathNotFoundException ignore) {
            log.warn("Failed to attach event listener for non existing path {}", absPath);
        } catch (final ItemNotFoundException ignore) {
            log.warn("Failed to attach event listener for for non existing item(s) {}", Arrays.toString(uuid));
        } catch (final RepositoryException e) {
            log.error("Failed to attach event listener", e);
        }
    }

    public void detach(final ObservationManager manager) {
        try {
            manager.removeEventListener(this);
        } catch (final RepositoryException e) {
            log.error("Failed to detach event listener, " + e.getMessage());
        }

    }
}
