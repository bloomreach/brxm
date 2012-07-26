/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.jca;

import javax.jcr.RepositoryException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

/**
 * This class implements the repository manager.
 */
public final class JCARepositoryManager {

    /**
     * Instance of manager.
     */
    private static final JCARepositoryManager INSTANCE =
            new JCARepositoryManager();

    /**
     * References.
     */
    private final Map references;

    /**
     * Flag indicating that the life cycle
     * of the resource is not managed by the
     * application server
     */
    private boolean autoShutdown = true;

    /**
     * Construct the manager.
     */
    private JCARepositoryManager() {
        this.references = new HashMap();
    }

    /**
     * Create repository.
     *
     * @param homeDir   The location of the repository.
     * @param configFile The path to the repository configuration file. If the file is located on
     *                   the classpath, the path should be prepended with
     *                   JCARepositoryManager.CLASSPATH_CONFIG_PREFIX.
     * @return repository instance
     */
    public HippoRepository createRepository(String location)
            throws RepositoryException {
        Reference ref = getReference(location);
        return ref.create();
    }

    /**
     * Shutdown all the repositories.
     */
    public void shutdown() {
        Collection references = this.references.values();
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            ref.shutdown();
        }
        this.references.clear();
    }

    /**
     * Return the reference.
     *
     * @param homeDir   The location of the repository.
     * @param configFile The path to the repository configuration file.
     */
    private synchronized Reference getReference(String location) {
        Reference ref = new Reference(location);
        Reference other = (Reference) references.get(ref);

        if (other == null) {
            references.put(ref, ref);
            return ref;
        } else {
            return other;
        }
    }

    /**
     * Return the instance.
     */
    public static JCARepositoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Repository reference implementation.
     */
    class Reference {
        /**
         * Home directory.
         */
        private final String location;

        /**
         * Repository instance.
         */
        private HippoRepository repository;

        /**
         * Construct the manager.
         */
        private Reference(String location) {
            this.location = location;
            this.repository = null;
        }

        /**
         * Return the repository.
         */
        public HippoRepository create()
                throws RepositoryException {
            if (repository == null) {
                if(location == null || location.trim().equals("")) {
                    repository = HippoRepositoryFactory.getHippoRepository();
                } else {
                    repository = HippoRepositoryFactory.getHippoRepository(location);
                }
            }
            return repository;
        }

        /**
         * Shutdown the repository.
         */
        public void shutdown() {
            if (repository != null) {
                repository.close();
            }
        }

        /**
         * Return the hash code.
         */
        @Override
        public int hashCode() {
            return location != null ? location.hashCode() : 0;
        }

        /**
         * Return true if equals.
         */
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            else if (o instanceof Reference) {
                if (location == null)
                    return ((Reference)o).location == null;
                else
                    return location.equals(((Reference)o).location);
            } else
                return false;
        }
    }

    public boolean isAutoShutdown() {
        return autoShutdown;
    }

    public void setAutoShutdown(boolean autoShutdown) {
        this.autoShutdown = autoShutdown;
    }

    /**
     * Try to shutdown the repository only if
     * {@link JCARepositoryManager#autoShutdown} is true.
     *
     * @param homeDir   The location of the repository.
     * @param configFile The path to the repository configuration file.
     */
    public void autoShutdownRepository(String location) {
        if (this.isAutoShutdown()) {
            Reference ref = getReference(location);
            ref.shutdown();
        }
    }
}


