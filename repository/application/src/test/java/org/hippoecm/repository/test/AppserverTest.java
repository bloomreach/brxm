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

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.TestCase;
import org.hippoecm.testutils.deployer.Deployer;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;

@RunWith(AppserverTest.class)
@Suite.SuiteClasses({
  org.hippoecm.repository.TrivialServerTest.class
})
public class AppserverTest extends Suite
{

    public AppserverTest(Class<?> klass) throws InitializationError {
        super(klass);
    }

    protected AppserverTest(Class<?> klass, Class<?>[] annotatedClasses) throws InitializationError {
        super(klass, annotatedClasses);
    }

    @Override
    public void run(final RunNotifier notifier) {
        Deployer deployer = null;
        HippoRepository server = null;
        String product = System.getProperties().getProperty("product");
        TestCase.clear();
        try {
            try {
                deployer = new Deployer();
                deployer.deploy(product);
            } catch(Exception ex) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(this.getClass().getName()), ex));
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            }

            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            TestCase.setRepository(server);

            super.run(notifier);

        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            if (server != null) {
                server.close();
            }
            try {
                if (deployer != null) {
                    deployer.undeploy(product);
                }
            } catch(Exception ex) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(this.getClass().getName()), ex));
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
}
