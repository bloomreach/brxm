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
package org.hippoecm.frontend;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.onehippo.repository.testutils.RepositoryTestCase;

@Ignore
@RunWith(RemoteTest.class)
@Suite.SuiteClasses({
    org.hippoecm.frontend.config.PluginConfigTest.class,
    org.hippoecm.frontend.model.JcrItemModelTest.class,
    org.hippoecm.frontend.model.JcrPropertyModelTest.class,
    //org.hippoecm.frontend.model.event.ObservationTest.class,
    org.hippoecm.frontend.model.ocm.JcrObjectTest.class,
    org.hippoecm.frontend.plugin.ServiceFactoryTest.class

})
public class RemoteTest extends Suite
{

    public RemoteTest(Class<?> klass) throws InitializationError {
        super(klass, PluginSuite.getAnnotatedClasses(klass));
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
            RepositoryTestCase.setRepository(server);

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
