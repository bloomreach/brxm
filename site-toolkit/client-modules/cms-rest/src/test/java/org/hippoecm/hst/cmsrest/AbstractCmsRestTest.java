/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cmsrest;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.testutils.RepositoryTestCase;

public abstract class AbstractCmsRestTest extends RepositoryTestCase {

    private SpringComponentManager componentManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        componentManager = new SpringComponentManager(getContainerConfiguration());
        componentManager.setConfigurationResources(getConfigurations());
        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(componentManager);

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        componentManager.stop();
        componentManager.close();
    }

    protected ComponentManager getComponentManager() {
        return componentManager;
    }

    protected org.apache.commons.configuration.Configuration getContainerConfiguration() {
        return new PropertiesConfiguration();
    }

    /**
     * required specification of spring configurations the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
        return new String[]{classXmlFileName, classXmlFileName2};
    }
}
