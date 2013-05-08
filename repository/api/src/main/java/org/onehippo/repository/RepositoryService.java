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
package org.onehippo.repository;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.SingletonService;

@SingletonService
public interface RepositoryService {

    /**
     * Authenticates the user using the supplied <code>credentials</code>.
     * If authentication succeeds the a {@link Session} object is returned that
     * supplies access to the default workspace.
     *
     * @param credentials  the credentials of the user
     * @return  a valid session for the user to access the repository.
     * @throws LoginException  if authentication or authorization for the specified workspace fails.
     * @throws RepositoryException  if another error occurs.
     */
    Session login(Credentials credentials) throws LoginException, RepositoryException;

}
