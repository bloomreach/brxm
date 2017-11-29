/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.observation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.observation.CmsEventDispatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmsEventDispatcherServiceImpl implements CmsEventDispatcherService, InternalCmsEventDispatcherService, Serializable {

    private static final Logger log = LoggerFactory.getLogger(CmsEventDispatcherServiceImpl.class);

    final transient ConcurrentMap<String, Set<JcrListener>> listeners = new ConcurrentHashMap<>();

    @Override
    public void events(final Node... nodes) {
        if (nodes == null) {
            return;
        }
        for (Node node : nodes) {
            try {
                final String path = node.getPath();
                final Set<JcrListener> jcrListeners = listeners.get(path);
                if (jcrListeners == null) {
                    continue;
                }
                final ChangeEvent changeEvent = new ChangeEvent(path, node.getSession().getUserID());
                for (JcrListener jcrListener : jcrListeners) {
                    jcrListener.onEvent(changeEvent);
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException ", e);
            }
        }
    }

    @Override
    public void subscribe(final String nodePath, final JcrListener jcrListener) {
        if (StringUtils.isEmpty(nodePath)) {
            return;
        }
        final boolean added = listeners.computeIfAbsent(nodePath, p -> new HashSet<>()).add(jcrListener);
        if (added) {
            log.debug("Added {} to listeners for path {}", jcrListener, nodePath);
        } else {
            log.debug("Listeners already contains {} for path {}", jcrListener, nodePath);
        }
    }

    @Override
    public void unsubscribe(final String nodePath, final JcrListener jcrListener) {
        if (StringUtils.isEmpty(nodePath)) {
            return;
        }
        Set<JcrListener> jcrListeners = listeners.get(nodePath);
        if (jcrListeners == null) {
            return;
        }
        synchronized (jcrListeners) {
            final boolean remove = jcrListeners.remove(jcrListener);
            if (remove) {
                log.debug("Removed {} for path {}", jcrListener, nodePath);
            } else {
                log.debug("Could not find {} for path {}", jcrListener, nodePath);
            }
            if (jcrListeners.isEmpty()) {
                synchronized (this) {
                    if (jcrListeners.isEmpty()) {
                        listeners.remove(nodePath);
                    }
                }
            }
        }
        logListenerInfo();
    }

    private void logListenerInfo() {
        if (log.isDebugEnabled()) {
            log.debug("To '{}' paths in total '{}' listeners are subscribed", listeners.size(),
                    listeners.values().stream().mapToInt(Set::size).sum());
        }
    }

}
