/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.integration;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventListener;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.jackrabbit.spi.Event;
import org.hippoecm.hst.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ServletContextAware;

public class AbstractHstIntegrationTest {

    protected SpringComponentManager componentManager;
    protected IntegrationHstManagerImpl hstManager;
    protected HstURLFactory hstURLFactory;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstEventsCollector hstEventsCollector;
    protected Object hstModelMutex;

    private Session localSession;
    private EventListener hstConfiglistener;

    @Before
    public void setUp() throws Exception {
        componentManager = new SpringComponentManager(getContainerConfiguration());
        componentManager.setConfigurationResources(getConfigurations());

        final MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath("/site");
        componentManager.setServletContext(servletContext);
        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());

        // register hst config changes listener
        localSession = createLocalSession(new SimpleCredentials("admin", "admin".toCharArray()));
        hstConfiglistener = HstServices.getComponentManager().getComponent("hstConfigurationEventListener");
        localSession.getWorkspace().getObservationManager().addEventListener(hstConfiglistener,
                Event.ALL_TYPES, "/hst:hst", true, null, null, false);

        hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        siteMapMatcher = HstServices.getComponentManager().getComponent(HstSiteMapMatcher.class.getName());
        hstURLFactory = HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());
        hstEventsCollector = HstServices.getComponentManager().getComponent("hstEventsCollector");
        hstModelMutex = HstServices.getComponentManager().getComponent("hstModelMutex");
    }

    @After
    public void tearDown() throws Exception {
        componentManager.stop();
        componentManager.close();

        // remove hst config changes listener
        localSession.getWorkspace().getObservationManager().removeEventListener(hstConfiglistener);
        localSession.logout();

        HstServices.setComponentManager(null);
        ModifiableRequestContextProvider.clear();
    }

    protected String[] getConfigurations() {
        String classXmlFileName = AbstractHstIntegrationTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractHstIntegrationTest.class.getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }


    protected Session createLocalSession(Credentials credentials) throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(credentials);
    }

    protected Session createRemoteSession(Credentials credentials) throws RepositoryException {
        final HippoRepository remoteRepository = HippoRepositoryFactory.
                getHippoRepository("rmi://127.0.0.1:1101/hipporepository");
        return remoteRepository.login(credentials);
    }

    protected Configuration getContainerConfiguration() {
        return new PropertiesConfiguration();
    }

}
