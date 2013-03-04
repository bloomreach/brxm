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
package org.onehippo.repository.modules;


import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * {@link DaemonModule} that needs module configuration.
 * A module config {@link javax.jcr.Node} is automatically passed in
 * with the {@link #configure} method on startup and when the module config changes.
 */
public interface ConfigurableDaemonModule extends DaemonModule {

    /**
     * Lifecycle callback to allow a {@link DaemonModule} to configure itself.
     * This method is called on startup iff there is module config node,
     * and before {@link #initialize} is called.
     *
     * @param moduleConfig  the node containing the configuration of this module
     * @throws javax.jcr.RepositoryException
     */
    void configure(Node moduleConfig) throws RepositoryException;

}
