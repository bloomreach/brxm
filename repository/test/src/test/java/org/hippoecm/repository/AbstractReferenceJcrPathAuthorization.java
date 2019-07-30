/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.onehippo.repository.testutils.RepositoryTestCase;

public abstract class AbstractReferenceJcrPathAuthorization extends RepositoryTestCase {


    protected void createAdminAuthRole(final Node pathFacetRuleDomain, final String user) throws RepositoryException {
        final Node bobIsAdmin = pathFacetRuleDomain.addNode(user, "hipposys:authrole");
        bobIsAdmin.setProperty("hipposys:users", new String[]{ user });
        bobIsAdmin.setProperty("hipposys:role", "admin");
    }

    protected Node createUser(String name) throws RepositoryException {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (!users.hasNode(name)) {
            final Node user = users.addNode(name, "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }
        return users;
    }

    protected Session loginUser(String user) throws RepositoryException {
        return server.login((Credentials)new SimpleCredentials(user, "password".toCharArray()));
    }

}
