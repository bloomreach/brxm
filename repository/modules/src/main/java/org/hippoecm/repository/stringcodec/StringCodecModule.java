/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.stringcodec;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.StringCodecService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

public class StringCodecModule extends AbstractReconfigurableDaemonModule {

    private final StringCodecModuleConfig config;
    private StringCodecService service;

    public StringCodecModule() {
        config = new StringCodecModuleConfig();
    }

    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        config.reconfigure(node);
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        service = new StringCodecServiceImpl(config);
        HippoServiceRegistry.registerService(service, StringCodecService.class);
    }

    @Override
    protected void doShutdown() {
        if (service != null) {
            HippoServiceRegistry.unregisterService(service, StringCodecService.class);
        }
    }
}
