/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.test;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.api.HippoSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class HREPTWO398Test
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    @Before
    public void setUp() {
        clear();
    }

    @After
    public void tearDown() {
        clear();
    }

    @Test
    public void testLocal() throws RepositoryException {
        HippoRepository server = HippoRepositoryFactory.getHippoRepository();
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        assertTrue(session instanceof HippoSession);
        session.logout();
        server.close();
    }

    @Test
    public void testRemote() throws RepositoryException, RemoteException, InterruptedException, AlreadyBoundException, MalformedURLException {
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

    static private void delete(File path) {
        if(path.exists()) {
            if(path.isDirectory()) {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++)
                    delete(files[i]);
            }
            path.delete();
        }
    }

    static private void clear() {
        String[] files = new String[] { ".lock", "repository", "version", "workspaces" };
        for(int i=0; i<files.length; i++) {
            File file = new File(files[i]);
            delete(file);
        }
    }
}
