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
package org.hippoecm.hst.test;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;

public class HstIntegrationTest extends RepositoryTestCase {

    protected Session sourceSession;
    protected Session targetSession;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        sourceSession = session;
        final HippoRepository remoteRepository = HippoRepositoryFactory.
                getHippoRepository("rmi://127.0.0.1:1101/hipporepository");
        targetSession = remoteRepository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    @Test
    public void testSanity() throws Exception {
        final String testId = sourceSession.getRootNode().addNode("test").getIdentifier();
        sourceSession.save();
        targetSession.refresh(false);
        assertEquals("/test", targetSession.getNodeByIdentifier(testId).getPath());
    }

}
