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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.model.HstModelImpl;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.mock.web.MockServletContext;

import static org.onehippo.cms7.services.context.HippoWebappContext.Type.CMS;
import static org.onehippo.cms7.services.context.HippoWebappContext.Type.SITE;

public abstract class AbstractComponentManagerTest {


    protected static final String CONTEXT_PATH = "/site";
    protected static final String PLATFORM_CONTEXT_PATH = "/cms";

    protected static SpringComponentManager siteComponentManager;
    protected static SpringComponentManager platformComponentManager;
    protected static final HippoWebappContext siteWebappContext = new HippoWebappContext(SITE, new MockServletContext() {
        public String getContextPath() {
            return CONTEXT_PATH;
        }
    });

    protected static final HippoWebappContext platformWebappContext = new HippoWebappContext(CMS, new MockServletContext() {
        public String getContextPath() {
            return PLATFORM_CONTEXT_PATH;
        }
    });

    public static final Set<String> annotatedClasses = new HashSet<>();
    public static final Set<String> extraPlatformAnnotatedClasses = new HashSet<>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");

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

    @AfterClass
    public static void tearDownClass() {
        siteComponentManager.stop();
        siteComponentManager.close();
        platformComponentManager.stop();
        platformComponentManager.close();
        HippoWebappContextRegistry.get().unregister(siteWebappContext);
        HippoWebappContextRegistry.get().unregister(platformWebappContext);
        HstServices.setComponentManager(null);
    }


    @After
    public void tearDown() throws Exception {

        ModifiableRequestContextProvider.clear();
        // model is used in next test method again, invalidate to have a clean empty model again
        ((HstModelImpl)platformComponentManager.getComponent(HstModelProvider.class).getHstModel()).invalidate();
        ((HstModelImpl)siteComponentManager.getComponent(HstModelProvider.class).getHstModel()).invalidate();
    }


    protected static String[] getConfigurations(boolean platform) {
        if (platform) {
           return Stream.of(annotatedClasses, extraPlatformAnnotatedClasses).flatMap(Set::stream).toArray(String[]::new);
        } else {
            return annotatedClasses.toArray(new String[0]);
        }
    }

    public static void addAnnotatedClassesConfigurationParam(final String annotatedClassParam) {
        annotatedClasses.add(annotatedClassParam);
    }

    public static void addPlatformAnnotatedClassesConfigurationParam(final String classXmlFileNamePlatform) {
        extraPlatformAnnotatedClasses.add(classXmlFileNamePlatform);
    }



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
