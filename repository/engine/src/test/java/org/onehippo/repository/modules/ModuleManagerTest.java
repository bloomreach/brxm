/*
 *  Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.modules;


import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.JcrConstants;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CMS_ONLY;

public class ModuleManagerTest extends RepositoryTestCase {

    private Node testModule;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node modules = session.getNode("/hippo:configuration/hippo:modules");
        testModule = modules.addNode("testmodule", HippoNodeType.NT_MODULE);
        testModule.addNode(HippoNodeType.HIPPO_MODULECONFIG, JcrConstants.NT_UNSTRUCTURED);
        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        if (testModule != null) {
            testModule.remove();
            session.save();
        }
        super.tearDown();
    }

    @Test
    public void testBasicLifeCycleManagement() throws Exception {
        testModule.setProperty("hipposys:className", BasicTestModule.class.getName());
        session.save();
        final ModuleManager moduleManager = new ModuleManager(session);
        final ModuleRegistration registration = moduleManager.registerModule(testModule);
        assertEquals(BasicTestModule.class, registration.getModuleClass());
        final BasicTestModule module = (BasicTestModule) registration.getModule();
        assertNotNull(module);
        moduleManager.startModule(registration);
        assertTrue("Module was expected to be initialized", module.initialized);
        moduleManager.stopModule(registration);
        assertTrue("Module was expected to be shut down", module.shutdown);
    }

    @Test
    public void testCMSRequiredModule() throws Exception {
        testModule.setProperty("hipposys:className", BasicTestModule.class.getName());
        testModule.setProperty(HIPPO_CMS_ONLY, true);
        session.save();
        {
            final ModuleManager moduleManager = new ModuleManager(session);
            final ModuleRegistration registration = moduleManager.registerModule(testModule);
            assertNull(registration);
        }
        {
            final ModuleManager moduleManager = new ModuleManager(session, true);
            final ModuleRegistration registration = moduleManager.registerModule(testModule);
            assertNotNull(registration);
        }
    }

    public static class BasicTestModule implements ConfigurableDaemonModule {

        private boolean initialized;
        private boolean shutdown;

        @Override
        public void configure(final Node moduleConfig) throws RepositoryException {
        }

        @Override
        public void initialize(final Session session) throws RepositoryException {
            initialized = true;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

    }
}
