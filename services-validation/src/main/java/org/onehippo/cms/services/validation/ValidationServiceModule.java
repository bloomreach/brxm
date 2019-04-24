/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import javax.jcr.Node;
import javax.jcr.Session;

import org.onehippo.cms.services.validation.api.ValidationService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = {ValidationService.class})
public class ValidationServiceModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(ValidationServiceModule.class);

    private ValidationServiceConfig config;
    private ValidationService service;

    @Override
    protected void doConfigure(final Node node) {
        if (config == null) {
            config = new ValidationServiceConfig(node);
        } else {
            config.reconfigure(node);
        }
        log.info("ValidationService (re)configured");
    }

    @Override
    protected void doInitialize(final Session session) {
        service = new ValidationServiceImpl(config);
        HippoServiceRegistry.register(service, ValidationService.class);
    }

    @Override
    protected void doShutdown() {
        if (service != null) {
            HippoServiceRegistry.unregister(service, ValidationService.class);
        }
    }
}
