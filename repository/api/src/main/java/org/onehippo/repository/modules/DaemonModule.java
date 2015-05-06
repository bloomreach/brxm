/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A DaemonModule represents a repository-managed component. DaemonModules
 * are started by the repository on startup. You can register your own
 * DaemonModule by declaring it in /hippo:configuration/hippo:modules
 *
 * @see ProvidesService
 * @see RequiresService
 * @see ConfigurableDaemonModule
 */
public interface DaemonModule {

    /**
     * Lifecycle callback method that is called when the component is started.
     *
     * @param session  a {@link Session} that can be used throughout this module's life.
     * @throws RepositoryException
     */
    public void initialize(Session session) throws RepositoryException;

    /**
     * Lifecycle callback method that is called by the repository before shutting down
     */
    public void shutdown();

}
