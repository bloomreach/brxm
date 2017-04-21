/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource;

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
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
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

import com.onehippo.cms7.crisp.api.CrispConstants;
import com.onehippo.cms7.crisp.api.resource.ResourceResolver;

public class RepositoryMapResourceResolverProvider extends MapResourceResolverProvider
        implements InitializingBean, DisposableBean, ApplicationContextAware {

    static Logger log = LoggerFactory.getLogger(RepositoryMapResourceResolverProvider.class);

    private ApplicationContext applicationContext;

    private Repository repository;
    private Credentials credentials;
    private boolean initialized;

    private ConfigurationChangeEventListener configurationChangeEventListener;

    private Map<String, AbstractApplicationContext> childAppContexts = new LinkedHashMap<>();

    public RepositoryMapResourceResolverProvider() {
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new ResourceResolversInitializingThread().start();
        configurationChangeEventListener = new ConfigurationChangeEventListener();
        HippoServiceRegistry.registerService(configurationChangeEventListener, HippoEventBus.class);
    }

    @Override
    public void destroy() {
        HippoServiceRegistry.unregisterService(configurationChangeEventListener, HippoEventBus.class);
    }

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
            log.error("Failed to initialize resource resolvers.", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

        return inited;
    }

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
