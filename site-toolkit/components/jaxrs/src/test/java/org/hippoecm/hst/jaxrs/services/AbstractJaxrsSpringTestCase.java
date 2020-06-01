/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.services;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletContext;

import static org.hippoecm.hst.content.tool.DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM;
import static org.onehippo.cms7.services.context.HippoWebappContext.Type.SITE;

/**
 * <p>
 * AbstractJaxrsSpringTestCase
 * </p>
 * @version $Id$
 */
public abstract class AbstractJaxrsSpringTestCase
{

    protected final static Logger log = LoggerFactory.getLogger(AbstractJaxrsSpringTestCase.class);
    
    protected SpringComponentManager componentManager;

    protected HippoWebappContext webappContext = new HippoWebappContext(SITE, new MockServletContext() {
        public String getContextPath() {
            return "/site";
        }
    });

    protected MockServletContext servletContext;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }

    @Before
    public void setUp() throws Exception {
        HippoWebappContextRegistry.get().register(webappContext);

        final Configuration containerConfiguration = getContainerConfiguration();
        containerConfiguration.addProperty("hst.configuration.rootPath", "/hst:hst");
        componentManager = new SpringComponentManager(containerConfiguration);
        componentManager.setConfigurationResources(getConfigurations());

        servletContext = new MockServletContext();
        servletContext.setContextPath("/site");

        servletContext.addInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM,
                "classpath*:org/hippoecm/hst/jaxrs/model/beans/**/*.class");

        List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
        if (!addonModuleDefinitions.isEmpty()) {
            componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
        }

        componentManager.setServletContext(servletContext);
        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.registerHstModel(servletContext, componentManager, true);
    }

    @After
    public void tearDown() throws Exception {
        HippoWebappContextRegistry.get().unregister(webappContext);
        this.componentManager.stop();
        this.componentManager.close();
        ModifiableRequestContextProvider.clear();
        HstServices.setComponentManager(null);
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";

        String classXmlFileNamePlatform = "org/hippoecm/hst/test/platform-context.xml";
        return new String[] { classXmlFileName, classXmlFileName2, classXmlFileNamePlatform };
    }
    
    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected <T> T getComponent(String name) {
        return getComponentManager().getComponent(name);
    }
    
    protected Configuration getContainerConfiguration() {
        return new PropertiesConfiguration();
    }
}
