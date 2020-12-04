/*
 * Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.repository.security.JvmCredentials;

public class JvmCredentialsLoginIT extends AbstractRepositoryTestCase {

    @Test
    public void test_jvm_credentials_login() throws RepositoryException {
        Session session1 = null;
        Session session2 = null;
        try {
            session1 = server.login(JvmCredentials.getCredentials("liveuser"));
            session2 = session1.getRepository().login(JvmCredentials.getCredentials("liveuser"));
        } finally {
            if (session1 != null) {
                session1.logout();
            }if (session2 != null) {
                session2.logout();
            }
        }

    }
}
