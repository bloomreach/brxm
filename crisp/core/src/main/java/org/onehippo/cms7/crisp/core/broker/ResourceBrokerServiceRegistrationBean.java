package org.onehippo.cms7.crisp.core.broker;

import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class ResourceBrokerServiceRegistrationBean implements InitializingBean, DisposableBean {

    private boolean enabled;
    private ResourceServiceBroker resourceServiceBroker;

    public ResourceBrokerServiceRegistrationBean() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ResourceServiceBroker getResourceServiceBroker() {
        return resourceServiceBroker;
    }

    public void setResourceServiceBroker(ResourceServiceBroker resourceServiceBroker) {
        this.resourceServiceBroker = resourceServiceBroker;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (isEnabled() && resourceServiceBroker != null) {
            HippoServiceRegistry.registerService(resourceServiceBroker, ResourceServiceBroker.class);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (isEnabled() && resourceServiceBroker != null) {
            HippoServiceRegistry.unregisterService(resourceServiceBroker, ResourceServiceBroker.class);
        }
    }
}
