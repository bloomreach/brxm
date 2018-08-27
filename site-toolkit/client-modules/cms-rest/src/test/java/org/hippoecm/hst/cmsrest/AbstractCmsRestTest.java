/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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


import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ServletContextAware;

public abstract class AbstractCmsRestTest {

    protected SpringComponentManager componentManager;
    protected HstManager hstManager;

    private MockServletContext servletContext;
    private MockServletContext servletContext2;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }

    @Before
    public void setUp() throws Exception {
        componentManager = new SpringComponentManager(getContainerConfiguration());
        componentManager.setConfigurationResources(getConfigurations());
        servletContext = new MockServletContext();
        servletContext.setContextPath("/site");
        ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.HST);

        servletContext2 = new MockServletContext();
        servletContext2.setContextPath("/site2");
        ServletContextRegistry.register(servletContext2, ServletContextRegistry.WebAppType.HST);
        componentManager.setServletContext(servletContext2);

        final List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
        if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
            componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
        }

        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());
        hstManager = getComponentManager().getComponent(HstManager.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
        ServletContextRegistry.unregister(servletContext);
        ServletContextRegistry.unregister(servletContext2);
        HstServices.setComponentManager(null);
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
