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

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.TestCase;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;

@RunWith(RemoteSPITest.class)
@Suite.SuiteClasses({
org.hippoecm.repository.HREPTWO2182Test.class
    // org.hippoecm.repository.FacetedAuthorizationTest.class  // failure
    // org.hippoecm.repository.CopyNodeTest.class // failure
    // org.hippoecm.repository.FacetedNavigationTest.class // failure
    // org.hippoecm.repository.HREPTWO456Test.class uuid
    // org.hippoecm.repository.HREPTWO475Test.class uuid
    // org.hippoecm.repository.HREPTWO650Test.class date
    // org.hippoecm.repository.HippoISMTest.class // docid
    // org.hippoecm.repository.HREPTWO283IssueTest.class uuid
    // org.hippoecm.repository.HREPTWO690Test.class uuid
    // org.hippoecm.repository.PendingChangesTest.class fails
    // org.hippoecm.repository.decorating.FacetedReferenceTest.class uuid
    // org.hippoecm.repository.decorating.PathsTest.class paths not set yet, needs save/invalidate?
    // org.hippoecm.repository.HREPTWO1493Test.class uuid
    // org.hippoecm.repository.HREPTWO548Test.class uuid
    // org.hippoecm.repository.decorating.SingledViewFacetSelectTest.class uuid


/*
    org.hippoecm.repository.TrivialServerTest.class, // ok
    // org.hippoecm.repository.FacetedAuthorizationTest.class,  // failure
    org.hippoecm.repository.RepositoryLoginTest.class, // ok
    org.hippoecm.repository.ConfigurationTest.class, // ok
    // org.hippoecm.repository.CopyNodeTest.class uuid
    org.hippoecm.repository.DerivedDataTest.class, // ok
    org.hippoecm.repository.FacetedNavigationChildNameTest.class, // ok
    org.hippoecm.repository.FacetedNavigationHippoCountTest.class, // ok
org.hippoecm.repository.FacetedNavigationNamespaceTest.class uuid
org.hippoecm.repository.FacetedNavigationPerfTest.class, // docid
    // org.hippoecm.repository.FacetedNavigationTest.class idem
org.hippoecm.repository.HREPTWO280Test.class, // docid
org.hippoecm.repository.HREPTWO425Test.class uuid
    org.hippoecm.repository.HREPTWO451Test.class, // ok
    // org.hippoecm.repository.HREPTWO456Test.class uuid
    // org.hippoecm.repository.HREPTWO475Test.class uuid
    // org.hippoecm.repository.HREPTWO650Test.class date
    // org.hippoecm.repository.HippoISMTest.class // docid
    // org.hippoecm.repository.HREPTWO283IssueTest.class uuid
org.hippoecm.repository.CanonicalPathTest.class  uuid
    // org.hippoecm.repository.HREPTWO690Test.class uuid
    org.hippoecm.repository.HippoNodeTypeSanityTest.class, // ok
    org.hippoecm.repository.HippoQueryTest.class, // ok
    // org.hippoecm.repository.PendingChangesTest.class fails
    // org.hippoecm.repository.decorating.FacetedReferenceTest.class uuid
    // org.hippoecm.repository.decorating.PathsTest.class paths not set yet, needs save/invalidate?
org.hippoecm.repository.decorating.MirrorTest.class uuid
    // org.hippoecm.repository.HREPTWO1493Test.class uuid
    // org.hippoecm.repository.HREPTWO548Test.class uuid
    org.hippoecm.repository.RepositoryMapTest.class // ok
    // org.hippoecm.repository.decorating.SingledViewFacetSelectTest.class uuid
*/
})
public class RemoteSPITest extends Suite
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public RemoteSPITest(Class<?> klass) throws InitializationError {
        super(klass);
    }

    protected RemoteSPITest(Class<?> klass, Class<?>[] annotatedClasses) throws InitializationError {
        super(klass, annotatedClasses);
    }

    @Override
    public void run(final RunNotifier notifier) {
        HippoRepositoryServer backgroundServer = null;
        HippoRepository server = null;
        try {
            backgroundServer = new HippoRepositoryServer();
            backgroundServer.run(true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository/spi");
            TestCase.setRepository(server);

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
        }
    }
}
