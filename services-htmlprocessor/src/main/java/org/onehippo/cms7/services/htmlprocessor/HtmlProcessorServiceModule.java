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
package org.onehippo.cms7.services.htmlprocessor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = {HtmlProcessorService.class})
public class HtmlProcessorServiceModule extends AbstractReconfigurableDaemonModule {

    public static final Logger log = LoggerFactory.getLogger(HtmlProcessorServiceModule.class);

    private final HtmlProcessorServiceConfig config;
    private HtmlProcessorService service;

    public HtmlProcessorServiceModule() {
        config = new HtmlProcessorServiceConfig();
    }

    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        config.reconfigure(node);
        log.info("HtmlProcessors (re)configured");
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        service = new HtmlProcessorServiceImpl(config);
        HippoServiceRegistry.register(service, HtmlProcessorService.class);
    }

    @Override
    protected void doShutdown() {
        if (service != null) {
            HippoServiceRegistry.unregister(service, HtmlProcessorService.class);
        }
    }
}
