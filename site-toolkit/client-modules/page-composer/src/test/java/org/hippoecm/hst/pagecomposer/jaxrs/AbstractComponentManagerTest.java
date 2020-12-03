/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs;

import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletContext;

import static org.onehippo.cms7.services.context.HippoWebappContext.Type.CMS;
import static org.onehippo.cms7.services.context.HippoWebappContext.Type.SITE;

public abstract class AbstractComponentManagerTest {


    private static final Logger log = LoggerFactory.getLogger(AbstractComponentManagerTest.class);

    protected static final String CONTEXT_PATH = "/site";
    protected static final String PLATFORM_CONTEXT_PATH = "/cms";

    protected SpringComponentManager siteComponentManager;
    protected SpringComponentManager platformComponentManager;
    protected final HippoWebappContext siteWebappContext = new HippoWebappContext(SITE, new MockServletContext() {
        public String getContextPath() {
            return CONTEXT_PATH;
        }
    });

    protected final HippoWebappContext platformWebappContext = new HippoWebappContext(CMS, new MockServletContext() {
        public String getContextPath() {
            return PLATFORM_CONTEXT_PATH;
        }
    });

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }


    @Before
    public void setUp() throws Exception {

        List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();

        HippoWebappContextRegistry.get().register(platformWebappContext);


        final PropertiesConfiguration platformConfiguration = new PropertiesConfiguration();
        platformConfiguration.addProperty("hst.configuration.rootPath", "/hst:platform");

        platformComponentManager = new SpringComponentManager(platformConfiguration);
        platformComponentManager.setConfigurationResources(getConfigurations(true));
        platformComponentManager.setServletContext(platformWebappContext.getServletContext());

        platformComponentManager.setAddonModuleDefinitions(addonModuleDefinitions);

        platformComponentManager.initialize();
        platformComponentManager.start();

        HstServices.setComponentManager(platformComponentManager);

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.registerHstModel(platformWebappContext.getServletContext(), platformComponentManager, true);

        final PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("hst.configuration.rootPath", "/hst:hst");
        siteComponentManager = new SpringComponentManager(configuration);
        siteComponentManager.setConfigurationResources(getConfigurations(false));

        HippoWebappContextRegistry.get().register(siteWebappContext);
        siteComponentManager.setServletContext(siteWebappContext.getServletContext());


        final List<ModuleDefinition> filteredModules = addonModuleDefinitions.stream().filter(moduleDefinition ->
                !moduleDefinition.getName().equals("org.hippoecm.hst.pagecomposer")
                        && !moduleDefinition.getName().equals("org.hippoecm.hst.platform")
                        && !moduleDefinition.getName().equals("org.hippoecm.hst.platform.test"))
                .collect(Collectors.toList());

        siteComponentManager.setAddonModuleDefinitions(filteredModules);

        siteComponentManager.initialize();
        siteComponentManager.start();

        modelRegistry.registerHstModel(siteWebappContext.getServletContext(), siteComponentManager, true);

    }

    @After
    public void tearDown() throws Exception {
        siteComponentManager.stop();
        siteComponentManager.close();
        platformComponentManager.stop();
        platformComponentManager.close();
        HippoWebappContextRegistry.get().unregister(siteWebappContext);
        HippoWebappContextRegistry.get().unregister(platformWebappContext);
        HstServices.setComponentManager(null);
        ModifiableRequestContextProvider.clear();
    }

    abstract protected String[] getConfigurations(boolean platform);

    public void createHstConfigBackup(Session session) throws RepositoryException {
        if (!session.nodeExists("/hst-backup")) {
            JcrUtils.copy(session, "/hst:hst", "/hst-backup");
            session.save();
        }
    }

    public void restoreHstConfigBackup(Session session) throws RepositoryException {
        if (session.nodeExists("/hst-backup")) {
            if (session.nodeExists("/hst:hst")) {
                session.removeItem("/hst:hst");
            }
            JcrUtils.copy(session, "/hst-backup", "/hst:hst");
            session.save();
        }
    }

}
