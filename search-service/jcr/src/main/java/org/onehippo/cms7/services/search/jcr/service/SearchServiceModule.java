/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.services.search.jcr.service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.search.service.QueryPersistService;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.onehippo.cms7.services.search.service.SearchServiceFactory;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconfigurable daemon module that registers the SearchServiceFactory.
 */
public class SearchServiceModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceModule.class);

    private SearchServiceFactory searchServiceFactory;

    private boolean wildcardPostfixEnabled;
    private int wildcardPostfixMinLength;

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        log.info("Initializing search service module");
    }

    private void register() {
        log.debug("Registering search service factory");
        searchServiceFactory = new SearchServiceFactory() {

            @Override
            public SearchService createSearchService(final Object clientObject) throws SearchServiceException {
                if (!(clientObject instanceof Session)) {
                    throw new SearchServiceException("Search service argument must be of type javax.jcr.Session");
                }
                return new HippoJcrSearchService((Session) clientObject, wildcardPostfixEnabled, wildcardPostfixMinLength);
            }

            @Override
            public QueryPersistService createQueryPersistService(final Object clientObject) throws SearchServiceException {
                if (!(clientObject instanceof Session)) {
                    throw new SearchServiceException("Search service argument must be of type javax.jcr.Session");
                }

                return new HippoJcrSearchService((Session) clientObject, wildcardPostfixEnabled, wildcardPostfixMinLength);
            }
        };

        HippoServiceRegistry.registerService(searchServiceFactory, SearchServiceFactory.class, SearchServiceFactory.class.getName());
    }

    @Override
    protected void doShutdown() {
        unregister();
    }

    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        unregister();

        this.wildcardPostfixEnabled = getBoolean(node, "wildcard.postfix.enabled", HippoJcrSearchService.DEFAULT_WILDCARD_POSTFIX_ENABLED);
        this.wildcardPostfixMinLength = getInteger(node, "wildcard.postfix.minlength", HippoJcrSearchService.DEFAULT_WILDCARD_POSTFIX_MINLENGTH);

        log.info("SearchServiceModule configured at wildcardPostfixEnabled={} and wildcardPostfixMinLength={}",
                this.wildcardPostfixEnabled, this.wildcardPostfixMinLength);

        register();
    }

    private void unregister() {
        log.debug("Unregistering search service factory {}", searchServiceFactory);
        if (searchServiceFactory != null) {
            HippoServiceRegistry.unregisterService(searchServiceFactory, SearchServiceFactory.class, SearchServiceFactory.class.getName());
        }
    }

    private int getInteger(final Node config, final String property, final int defaultValue) throws RepositoryException {
        if (config.hasProperty(property)) {
            return (int)config.getProperty(property).getLong();
        }
        return defaultValue;
    }

    private boolean getBoolean(final Node config, final String property, final boolean defaultValue) throws RepositoryException {
        if (config.hasProperty(property)) {
            return config.getProperty(property).getBoolean();
        }
        return defaultValue;
    }
}
