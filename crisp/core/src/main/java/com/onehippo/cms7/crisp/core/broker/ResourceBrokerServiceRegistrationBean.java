/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.broker;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;

/**
 * Bean implementation to register {@link ResourceServiceBroker} in {@link HippoServiceRegistry}.
 */
public class ResourceBrokerServiceRegistrationBean implements InitializingBean, DisposableBean {

    /**
     * Whether or not this service registration should be enabled.
     */
    private boolean enabled;

    /**
     * The singleton {@link ResourceServiceBroker} instance to register.
     */
    private ResourceServiceBroker resourceServiceBroker;

    /**
     * Default constructor.
     */
    public ResourceBrokerServiceRegistrationBean() {
    }

    /**
     * Returns true if this service registration should be enabled.
     * @return true if this service registration should be enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the flag whether or not this service registration should be enabled.
     * @param enabled the flag whether or not this service registration should be enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the singleton {@link ResourceServiceBroker} instance.
     * @return the singleton {@link ResourceServiceBroker} instance
     */
    public ResourceServiceBroker getResourceServiceBroker() {
        return resourceServiceBroker;
    }

    /**
     * Sets the singleton {@link ResourceServiceBroker} instance.
     * @param resourceServiceBroker the singleton {@link ResourceServiceBroker} instance
     */
    public void setResourceServiceBroker(ResourceServiceBroker resourceServiceBroker) {
        this.resourceServiceBroker = resourceServiceBroker;
    }

    /**
     * {@inheritDoc}
     * <P>
     * This method is overridden to register the singleton {@link ResourceServiceBroker} instance in {@link HippoServiceRegistry}.
     * </P>
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (isEnabled() && resourceServiceBroker != null) {
            HippoServiceRegistry.registerService(resourceServiceBroker, ResourceServiceBroker.class);
        }
    }

    /**
     * {@inheritDoc}
     * <P>
     * This method is overridden to unregister the singleton {@link ResourceServiceBroker} instance in {@link HippoServiceRegistry}.
     * </P>
     */
    @Override
    public void destroy() throws Exception {
        if (isEnabled() && resourceServiceBroker != null) {
            HippoServiceRegistry.unregisterService(resourceServiceBroker, ResourceServiceBroker.class);
        }
    }
}
