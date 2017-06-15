/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRISP Repository Addon <code>DaemonModule</code> implementation that produces {@link HippoEvent} on any module
 * configuration change through {@link HippoEventBus}.
 */
public class CrispRegistryModule extends AbstractReconfigurableDaemonModule {

    private static Logger log = LoggerFactory.getLogger(CrispRegistryModule.class);

    /**
     * <code>HippoEvent</code> application name used internally in configuration change <code>HippoEvent</code> handling.
     */
    private static final String EVENT_APPLICATION_NAME = "crisp";

    /**
     * <code>HippoEvent</code> category name used internally in configuration change <code>HippoEvent</code> handling.
     */
    private static final String EVENT_CATEGORY_CONFIGURATION = "configuration";

    /**
     * <code>HippoEvent</code> action name used internally in configuration change <code>HippoEvent</code> handling.
     */
    private static final String EVENT_ACTION_UPDATE_CONFIGURATION = "updateConfiguration";

    /**
     * Flag whether or not the module configuration was changed.
     */
    private boolean configurationUpdated;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doConfigure(Node moduleConfig) throws RepositoryException {
        configurationUpdated = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        if (configurationUpdated) {
            configurationUpdated = false;
            postConfigurationChangeEvent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        super.onConfigurationChange(moduleConfig);
        doInitialize(moduleConfig.getSession());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doShutdown() {
    }

    /**
     * Posts a {@link HippoEvent} through {@link HippoEventBus} about a module configuration change event.
     */
    private void postConfigurationChangeEvent() {
        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);

        if (eventBus == null) {
            log.warn("HippoEventBus is not available.");
            return;
        }

        HippoEvent event = new HippoEvent(EVENT_APPLICATION_NAME);
        event.category(EVENT_CATEGORY_CONFIGURATION)
                .action(EVENT_ACTION_UPDATE_CONFIGURATION);
        eventBus.post(event);
    }

}
