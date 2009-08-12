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

import org.hippoecm.testutils.deployer.Deployer;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;

@RunWith(IntegrationTest.class)
@Suite.SuiteClasses( { SmokeTest.class })
public class IntegrationTest extends Suite {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public IntegrationTest(Class<?> klass) throws InitializationError {
        super(klass);
    }

    protected IntegrationTest(Class<?> klass, Class<?>[] annotatedClasses) throws InitializationError {
        super(klass, annotatedClasses);
    }

    @Override
    public void run(final RunNotifier notifier) {
        Deployer deployer = null;
        String product = System.getProperties().getProperty("product");
        try {
            try {
                deployer = new Deployer();
                deployer.deploy(product);
            } catch (Exception ex) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(this.getClass().getName()), ex));
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }

            super.run(notifier);

        } finally {
            try {
                if (deployer != null) {
                    deployer.undeploy(product);
                }
            } catch (Exception ex) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(this.getClass().getName()), ex));
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
}
