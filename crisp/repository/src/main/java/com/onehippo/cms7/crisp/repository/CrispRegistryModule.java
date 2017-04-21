/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onehippo.cms7.crisp.api.CrispConstants;

public class CrispRegistryModule extends AbstractReconfigurableDaemonModule {

    private static Logger log = LoggerFactory.getLogger(CrispRegistryModule.class);

    private boolean configurationUpdated;

    @Override
    protected void doConfigure(Node moduleConfig) throws RepositoryException {
        configurationUpdated = true;
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        if (configurationUpdated) {
            configurationUpdated = false;
            postConfigurationChangeEvent();
        }
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        super.onConfigurationChange(moduleConfig);
        doInitialize(moduleConfig.getSession());
    }

    @Override
    protected void doShutdown() {
    }

    private void postConfigurationChangeEvent() {
        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);

        if (eventBus == null) {
            log.warn("HippoEventBus is not available.");
            return;
        }

        HippoEvent event = new HippoEvent(CrispConstants.EVENT_APPLICATION_NAME);
        event.category(CrispConstants.EVENT_CATEGORY_CONFIGURATION)
                .action(CrispConstants.EVENT_ACTION_UPDATE_CONFIGURATION);
        eventBus.post(event);
    }

}
