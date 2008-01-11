/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.test;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.api.HippoSession;

public class HREPTWO398Test extends TestCase
{
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    public void testLocal() throws RepositoryException {
        HippoRepository server = HippoRepositoryFactory.getHippoRepository();
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        assertTrue(session instanceof HippoSession);
        session.logout();
        server.close();
    }

    public void testRemote() throws RepositoryException, RemoteException, InterruptedException, AlreadyBoundException {
        HippoRepositoryServer backgroundServer = new HippoRepositoryServer();
        backgroundServer.run(true);
        Thread.sleep(3000);
        HippoRepository server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        assertTrue(session instanceof HippoSession);
        session.logout();
        server.close();
        backgroundServer.close();
        Thread.sleep(3000);
    }
}
