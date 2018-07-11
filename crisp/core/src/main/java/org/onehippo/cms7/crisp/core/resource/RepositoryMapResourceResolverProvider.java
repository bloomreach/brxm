/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.crisp.api.CrispConstants;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.util.InMemoryResource;

/**
 * Extending {@link MapResourceResolverProvider} to filling in the internal <code>Map</code> of pairs of <strong>resource space</strong>
 * name and {@link ResourceResolver} instance by reading configurations in the repository.
 */
public class RepositoryMapResourceResolverProvider extends MapResourceResolverProvider
        implements InitializingBean, DisposableBean, ApplicationContextAware {

    static Logger log = LoggerFactory.getLogger(RepositoryMapResourceResolverProvider.class);

    /**
     * Spring ApplicationContext which instantiates this bean.
     */
    private ApplicationContext applicationContext;

    /**
     * Repository storing configurations.
     */
    private Repository repository;

    /**
     * Credentials to log in the {@link #repository}.
     */
    private Credentials credentials;

    /**
     * Flag whether or not this bean was initialized successfully.
     */
    private boolean initialized;

    /**
     * {@link HippoEventBus} event listener instance to subscribe configuration changes in the repository.
     */
    private ConfigurationChangeEventListener configurationChangeEventListener;

    /**
     * Map of pairs of <strong>resource space</strong> name and {@link AbstractApplicationContext} instance.
     */
    private Map<String, AbstractApplicationContext> childAppContexts = new LinkedHashMap<>();

    /**
     * Default constructor.
     */
    public RepositoryMapResourceResolverProvider() {
    }

    /**
     * Return the repository storing configurations.
     * @return the repository storing configurations
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Sets the repository storing configurations.
     * @param repository the repository storing configurations
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * Returns the credentials to log in the {@link #repository}.
     * @return the credentials to log in the {@link #repository}
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Sets the credentials to log in the {@link #repository}.
     * @param credentials the credentials to log in the {@link #repository}
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        new ResourceResolversInitializingThread().start();
        configurationChangeEventListener = new ConfigurationChangeEventListener();
        HippoEventListenerRegistry.get().register(configurationChangeEventListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        HippoEventListenerRegistry.get().unregister(configurationChangeEventListener);
    }

    /**
     * {@link HippoEventBus} event listener to subscribe configuration changes in the repository and initialize
     * the {@link ResourceResolver}s for each <strong>resource space</strong>s.
     */
    public class ConfigurationChangeEventListener {
        @Subscribe
        public void handleEvent(HippoEvent event) {
            if (CrispConstants.EVENT_APPLICATION_NAME.equals(event.application())
                    && CrispConstants.EVENT_CATEGORY_CONFIGURATION.equals(event.category())
                    && CrispConstants.EVENT_ACTION_UPDATE_CONFIGURATION.equals(event.action())) {
                initializeResourceResolvers();
            }
        }
    }

    /**
     * Initializes the internal map of {@link ResourceResolver}s for each <strong>resource space</strong>, and
     * returns true if the initialization was successful.
     * @return initializes the internal map of {@link ResourceResolver}s for each <strong>resource space</strong>, and
     * returns true if the initialization was successful.
     */
    private boolean initializeResourceResolvers() {
        boolean inited = false;
        Session session = null;

        try {
            session = repository.login(credentials);

            if (session.nodeExists(CrispConstants.REGISTRY_MODULE_PATH)) {
                Node moduleNode = session.getNode(CrispConstants.REGISTRY_MODULE_PATH);

                if (moduleNode.hasNode("hippo:moduleconfig")) {
                    Node moduleConfigNode = moduleNode.getNode("hippo:moduleconfig");

                    if (!moduleConfigNode.isNodeType(CrispConstants.NT_MODULE_CONFIG)) {
                        log.error("CRISP module configuration node must be type of '{}'.",
                                CrispConstants.NT_MODULE_CONFIG);
                        return false;
                    }

                    if (moduleConfigNode.hasNode(CrispConstants.RESOURCE_RESOLVER_CONTAINER)) {
                        Node resourceReesolverContainerNode = moduleConfigNode
                                .getNode(CrispConstants.RESOURCE_RESOLVER_CONTAINER);
                        Map<String, ResourceResolver> tempResourceResolversMap = new LinkedHashMap<>();

                        Node resourceResolverNode;
                        String resourceSpace;
                        Set<String> resourceSpaceNames = new HashSet<>();
                        String beanDef;
                        String[] propNames;
                        String[] propValues;
                        AbstractApplicationContext newChildContext;
                        AbstractApplicationContext oldChildContext;
                        ResourceResolver resourceResolver;

                        for (NodeIterator nodeIt = resourceReesolverContainerNode.getNodes(); nodeIt.hasNext();) {
                            resourceResolverNode = nodeIt.nextNode();

                            if (resourceResolverNode != null) {
                                resourceSpace = resourceResolverNode.getName();
                                beanDef = JcrUtils.getStringProperty(resourceResolverNode,
                                        CrispConstants.BEAN_DEFINITION, null);
                                propNames = JcrUtils.getMultipleStringProperty(resourceResolverNode,
                                        CrispConstants.PROP_NAMES, null);
                                propValues = JcrUtils.getMultipleStringProperty(resourceResolverNode,
                                        CrispConstants.PROP_VALUES, null);

                                if (StringUtils.isNotBlank(beanDef)) {
                                    try {
                                        newChildContext = createChildApplicationContext(beanDef, propNames, propValues);
                                        resourceSpaceNames.add(resourceSpace);
                                        resourceResolver = newChildContext.getBean(ResourceResolver.class);
                                        tempResourceResolversMap.put(resourceSpace, resourceResolver);
                                        oldChildContext = childAppContexts.put(resourceSpace, newChildContext);

                                        if (oldChildContext != null) {
                                            oldChildContext.close();
                                        }
                                    } catch (Exception childContextEx) {
                                        log.error("Failed to load child context for resource space, '{}'.",
                                                resourceSpace, childContextEx);
                                    }
                                }
                            }
                        }

                        for (String resourceSpaceNameInChildContexts : childAppContexts.keySet()) {
                            if (!resourceSpaceNames.contains(resourceSpaceNameInChildContexts)) {
                                oldChildContext = childAppContexts.get(resourceSpaceNameInChildContexts);
                                if (oldChildContext != null) {
                                    oldChildContext.close();
                                }
                            }
                        }

                        setResourceResolverMap(tempResourceResolversMap);

                        log.warn("CRISP resource resolvers map initialized: {}", getResourceResolverMap());
                    }
                }

                inited = true;
            }
        } catch (RepositoryException e) {
            log.warn("Cannot initialize resource resolvers (yet). Perhaps the repository wasn't initialized yet. {}", e.toString());
        } finally {
            if (session != null) {
                session.logout();
            }
        }

        return inited;
    }

    /**
     * Creates a child {@link ApplicationContext} for a <strong>resource space</strong>.
     * @param beanDefs bean definitions in XML
     * @param propNames variable property names
     * @param propValues variable property values.
     * @return
     */
    private AbstractApplicationContext createChildApplicationContext(final String beanDefs, String[] propNames,
            String[] propValues) {
        GenericApplicationContext childContext = new GenericApplicationContext(applicationContext);
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(childContext);
        xmlReader.loadBeanDefinitions(new InMemoryResource(beanDefs));

        Properties props = new Properties();

        if (propNames != null && propValues != null) {
            for (int i = 0; i < propNames.length; i++) {
                if (propValues.length > i) {
                    props.setProperty(propNames[i], propValues[i]);
                }
            }
        }

        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setIgnoreUnresolvablePlaceholders(true);
        ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
        ppc.setProperties(props);
        childContext.addBeanFactoryPostProcessor(ppc);

        childContext.refresh();

        return childContext;
    }

    /**
     * Thread to initialize the internal map of {@link ResourceResolver}s for each <strong>resource space</strong>
     * asynchronously.
     */
    private class ResourceResolversInitializingThread extends Thread {

        ResourceResolversInitializingThread() {
            super("ResourceResolversInitializingThread");
        }

        public void run() {
            while (!initialized) {
                if (initializeResourceResolvers()) {
                    initialized = true;
                    break;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.warn("Thread, '{}', was interrupted.", getName());
                    break;
                }
            }
        }
    }

}
