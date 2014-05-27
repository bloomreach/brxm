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


import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.MutableHstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractCmsRestTest {

    protected ComponentManager componentManager;

    @Before
    public void setUp() throws Exception {
        this.componentManager = new SpringComponentManager(getContainerConfiguration());
        this.componentManager.setConfigurationResources(getConfigurations());

        this.componentManager.initialize();
        this.componentManager.start();
        HstServices.setComponentManager(getComponentManager());
        ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                .setContextPath("/site");
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
        HstServices.setComponentManager(null);
        ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                .setContextPath(null);
        // always clear HstRequestContext in case it is set on a thread local
        ModifiableRequestContextProvider.clear();
    }

    protected String[] getConfigurations() {
        String classXmlFileName = AbstractCmsRestTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractCmsRestTest.class.getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }


    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    protected Configuration getContainerConfiguration() {
        return new PropertiesConfiguration();
    }

}
