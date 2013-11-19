/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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


package org.hippoecm.hst.configuration.cache;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * <p>
 *   Note that this class is <strong>not</strong> thread-safe : It should not be accessed by concurrent threads
 * </p>
 */
public class HstNodeLoadingCache implements HstEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(HstNodeLoadingCache.class);

    private String rootPath;
    private String rootConfigurationsPrefix;
    private int rootPathLength;
    private HstNode rootNode;
    private Repository repository;
    private String username;
    private String password;

    private Set<HstEvent> events;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
        rootPathLength = rootPath.length();
        rootConfigurationsPrefix = rootPath + "/hst:configurations/";
    }

    @Override
    public void handleEvents(final Set<HstEvent> events) {
        if (this.events != null) {
            this.events.addAll(events);
        } else {
            // copy the events to a new set as from argument is an immutable set
            this.events = new HashSet<>(events);
        }
    }

    public String getRootPath() {
        return rootPath;
    }

    /**
     *
     * @param absPath
     * @return the node for absPath and <code>null</code> otherwise
     * @throws IllegalArgumentException is absPath does not start with <code>rootPath + /</code>
     */
    public HstNode getNode(String absPath) {
        long getNodeStartTime = System.currentTimeMillis();
        if (!absPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("Path for getting node from hst node cache must start with rootPath " +
                    "'"+rootPath+"' but was '"+absPath+"'.");
        }
        try (LazyCloseableSession lazyCloseableSession = createLazyCloseableSession()) {
            if (rootNode == null) {
                rootNode = new HstNodeImpl(lazyCloseableSession.getSession().getNode(rootPath), null);
                events = null;
            } else if (events != null) {
                // reload only certain parts
                long start = System.currentTimeMillis();
                for (HstEvent event : events) {
                    HstNode nodeForEvent = fetchHstNode(event.getNodePath());
                    boolean orderedReload;
                    if (event.isPropertyEvent() && nodeForEvent != null) {
                        // if already marked stale due to 'node event' we should not mark it now stale
                        // due to property event.
                        // only reload the value provider, this can always be done order agnostic
                        nodeForEvent.markStaleByPropertyEvent();
                    } else {
                        if (nodeForEvent != null) {
                            orderedReload = orderMatters(nodeForEvent);
                            if (orderedReload) {
                                // since the order might be changed, reload the parent
                                nodeForEvent = getFirstNonNullAncestor(event.getNodePath());
                            }
                            nodeForEvent.markStaleByNodeEvent(orderedReload);
                        } else {
                            // find first non null ancestor
                            HstNode ancestorNodeForEvent = getFirstNonNullAncestor(event.getNodePath());
                            orderedReload = orderMatters(ancestorNodeForEvent);
                            ancestorNodeForEvent.markStaleByNodeEvent(orderedReload);
                        }
                    }
                }
                log.info("Processing '{}' events took '{}' ms.", events.size(), (System.currentTimeMillis() - start));
                // events all processed. Mark as null
                events = null;
                // all items that need reloading are now marked. The HstNode root can update itself now
                start = System.currentTimeMillis() ;
                rootNode.update(lazyCloseableSession.getSession());
                log.info("Updating root HstNode took '{}' ms.", (System.currentTimeMillis() - start));
            }
        } catch (RepositoryException e) {
            throw new ModelLoadingException("Could not load hst node model due to RepositoryException : ", e);
        }
        HstNode result;
        if (absPath.equals(rootPath)) {
            result = rootNode;
        } else {
            result = rootNode.getNode(absPath.substring(rootPathLength + 1));
        }
        log.info("Getting HstNode '{}' took '{}' ms.",absPath,  (System.currentTimeMillis() - getNodeStartTime));
        return result;
    }

    private boolean orderMatters(final HstNode node) {
        if (node.getValueProvider().getPath().startsWith(rootConfigurationsPrefix)) {
            // nodes directly below hst:configurations are not important to reload their
            // children in order, as that can be very expensive to reload them all. Hence extra check
            // whether it is not a direct child
            if (node.getValueProvider().getPath().substring(rootConfigurationsPrefix.length()).contains("/")) {
                return true;
            }
        }
        return false;
    }


    /**
     * @return the <code>HstNode</code> for <code>absPath</code> and <code>null</code> if non existing
     */
    private HstNode fetchHstNode(final String absPath) {
        if (!absPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("Invalid absPath because does not start with '"+rootPath+"'");
        }
        if (absPath.equals(rootPath)) {
            return rootNode;
        }
        return rootNode.getNode(absPath.substring(rootPathLength + 1));
    }

    private HstNode getFirstNonNullAncestor(final String absPath) {
        if (!absPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("Invalid absPath because does not start with '"+rootPath+"'");
        }
        if (absPath.equals(rootPath)) {
            return rootNode;
        }
        String absParentPath = StringUtils.substringBeforeLast(absPath, "/");
        if (absParentPath.equals(rootPath)) {
            return rootNode;
        }
        HstNode node = rootNode.getNode(absParentPath.substring(rootPathLength + 1));
        if (node != null) {
            return node;
        }
        return getFirstNonNullAncestor(absParentPath);
    }

    public LazyCloseableSession createLazyCloseableSession() {
        return new LazyCloseableSession();
    }

    public class LazyCloseableSession implements AutoCloseable {

        Session session;

        public Session getSession() {
            if (session != null) {
                return session;
            }
            try {
                session = repository.login(new SimpleCredentials(username, password.toCharArray()));
            } catch (RepositoryException e) {
                throw new ModelLoadingException("Repository exception while getting jcr session", e);
            }
            return session;
        }

        @Override
        public void close() throws RepositoryException {
            if (session != null) {
                session.logout();
            }
        }
    }

}
