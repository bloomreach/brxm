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
package org.hippoecm.repository.api;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A reference workspace represents the state of the repository after it was
 * first bootstrapped. It can be used to determine what changed since then.
 */
public interface ReferenceWorkspace {

    /**
     * Log in to the workspace with system credentials
     *
     * @return  a {@link Session} for accessing the workspace
     * @throws RepositoryException
     */
    Session login() throws RepositoryException;

    /**
     * Initializes the workspace with the content it finds on the classpath.
     * @throws RepositoryException
     * @throws IOException
     */
    void bootstrap() throws RepositoryException, IOException;

    /**
     * Removes all nodes from the workspace except for the system nodes.
     * @throws RepositoryException
     */
    void clean() throws RepositoryException;

}
