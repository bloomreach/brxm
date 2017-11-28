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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.observation.CmsEventDispatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmsEventDispatcherServiceImpl implements CmsEventDispatcherService, InternalCmsEventDispatcherService {

    private final static Logger log = LoggerFactory.getLogger(CmsEventDispatcherServiceImpl.class);

    final Map<String, Set<JcrListener>> listeners = new ConcurrentHashMap<>();

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
                for (JcrListener jcrListener : jcrListeners) {
                    jcrListener.onEvent(new ChangeEvent(path, node.getSession().getUserID()));
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
        Set<JcrListener> jcrListeners = listeners.get(nodePath);
        if (jcrListeners == null) {
            synchronized (this) {
                jcrListeners = listeners.get(nodePath);
                if (jcrListeners == null) {
                    // note do NOT use here org.apache.wicket.util.collections.ConcurrentHashSet because there can be
                    // *many* instances (for every nodePath) AND ConcurrentHashSet is backed by a ConcurrentHashMap which
                    // used to have a very large memory footprint (note sure how much improved in java 8. To be save, use
                    // normal hashset and synchronize on jcrListeners
                    jcrListeners = new HashSet<>();
                    listeners.put(nodePath, jcrListeners);
                }
            }
        }
        synchronized (jcrListeners) {
            if (jcrListeners.contains(jcrListener)) {
                log.debug("Listeners already contains {} for path {}", jcrListener, nodePath);
                return;
            }
            log.debug("Adding {} to listeners for path {}", jcrListener, nodePath);
            jcrListeners.add(jcrListener);
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
            log.debug("To #{} paths in total '{}' listeners are subscribed", listeners.size(),
                    listeners.values().stream().mapToInt(value -> value.size()).sum());
        }
    }

}
