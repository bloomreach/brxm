/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.SecurityServiceTest;
import org.junit.Ignore;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.onehippo.repository.testutils.RepositoryTestCase;

/**
 * Run tests remotely (over RMI).  These tests should not start/stop and/or clean up the repository.
 */
@RunWith(RemoteTest.class)
@Suite.SuiteClasses({
    org.hippoecm.repository.CanonicalPathTest.class,
    org.hippoecm.repository.CopyNodeTest.class,
    org.hippoecm.repository.DerivedDataTest.class,
    org.hippoecm.repository.DescriptorsTest.class,
    org.hippoecm.repository.PendingChangesTest.class,
    org.hippoecm.repository.RepositoryMapTest.class,
    org.hippoecm.repository.FacetedNavigationChildNameTest.class,
    org.hippoecm.repository.FacetedNavigationHippoCountTest.class,
    org.hippoecm.repository.FacetedNavigationNamespaceTest.class,
    org.hippoecm.repository.FacetedNavigationTest.class,
    org.hippoecm.repository.HREPTWO280Test.class,
    org.hippoecm.repository.HREPTWO283IssueTest.class,
    org.hippoecm.repository.HREPTWO690Test.class,
    // org.hippoecm.repository.HREPTWO1493Test.class,
    org.hippoecm.repository.HREPTWO650Test.class,
    org.hippoecm.repository.HREPTWO3402IssueTest.class,
    org.hippoecm.repository.decorating.FacetedReferenceTest.class,
    org.hippoecm.repository.decorating.MirrorTest.class,
    org.hippoecm.repository.decorating.PathsTest.class,
    org.hippoecm.repository.decorating.SingledViewFacetSelectTest.class,
//  org.hippoecm.frontend.model.event.ObservationTest.class
    org.hippoecm.repository.HREPTWO4999Test.class
})
@Ignore
public class RemoteTest extends Suite
{

    public RemoteTest(Class<?> klass) throws InitializationError {
        super(klass);
    }

    protected RemoteTest(Class<?> klass, Class<?>[] annotatedClasses) throws InitializationError {
        super(klass, annotatedClasses);
    }

    @Override
    public void run(final RunNotifier notifier) {
        HippoRepositoryServer backgroundServer = null;
        HippoRepository server = null;
        try {
            RepositoryTestCase.clearRepository();
            backgroundServer = new HippoRepositoryServer();
            backgroundServer.run(true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            RepositoryTestCase.setRepository(server);
            HippoRepositoryFactory.setDefaultRepository((String) null);

            super.run(notifier);

        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(RemoteException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(java.rmi.AlreadyBoundException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(InterruptedException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (MalformedURLException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            if (server != null) {
                server.close();
            }
            if (backgroundServer != null) {
                backgroundServer.close();
            }
            RepositoryTestCase.clearRepository();
            RepositoryTestCase.setRepository(null);
            HippoRepositoryFactory.setDefaultRepository((String)null);
        }
    }
}
