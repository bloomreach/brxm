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

import java.io.File;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;

import org.hippoecm.repository.TestCase;

import org.junit.*;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import static org.junit.Assert.*;

@RunWith(RemoteTest.class)
@Suite.SuiteClasses({
  org.hippoecm.repository.TrivialServerTest.class,
  /*
  org.hippoecm.repository.CanonicalPathTest.class,
  org.hippoecm.repository.ConfigurationTest.class,
  org.hippoecm.repository.CopyNodeTest.class,
  org.hippoecm.repository.DerivedDataTest.class,
  org.hippoecm.repository.FacetedAuthorizationTest.class,
  org.hippoecm.repository.FacetedNavigationAbstractTest.class,
  org.hippoecm.repository.FacetedNavigationChildNameTest.class,
  org.hippoecm.repository.FacetedNavigationHippoCountTest.class,
  org.hippoecm.repository.FacetedNavigationNamespaceTest.class,
  org.hippoecm.repository.FacetedNavigationPerfTest.class,
  org.hippoecm.repository.FacetedNavigationTest.class,
  org.hippoecm.repository.HREPIssueTest.class,
  org.hippoecm.repository.HREPTWO280Test.class,
  org.hippoecm.repository.HREPTWO283IssueTest.class,
  org.hippoecm.repository.HREPTWO425Test.class,
  org.hippoecm.repository.HREPTWO451Test.class,
  org.hippoecm.repository.HREPTWO456Test.class,
  org.hippoecm.repository.HREPTWO475Test.class,
  org.hippoecm.repository.HREPTWO650Test.class,
  org.hippoecm.repository.HREPTWO690Test.class,
  org.hippoecm.repository.HippoISMTest.class,
  org.hippoecm.repository.HippoNodeTypeSanityTest.class,
  org.hippoecm.repository.HippoQueryTest.class,
  org.hippoecm.repository.PendingChangesTest.class,
  org.hippoecm.repository.PhysicalExportSystemView.class,
  org.hippoecm.repository.RepositoryLoginTest.class,
  org.hippoecm.repository.RepositoryRolesTest.class,
  org.hippoecm.repository.TransactionTest.class,
  org.hippoecm.repository.TrivialServerTest.class,
  org.hippoecm.repository.decorating.FacetedReferenceTest.class,
  org.hippoecm.repository.decorating.MirrorTest.class,
  org.hippoecm.repository.decorating.PathsTest.class,
  */
  org.hippoecm.repository.decorating.SingledViewFacetSelectTest.class
})
public class RemoteTest extends Suite
    // used to extends junit.framework.TestCase
{
    private final static String SVN_ID = "$Id$";

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
            backgroundServer = new HippoRepositoryServer();
            backgroundServer.run(true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
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
