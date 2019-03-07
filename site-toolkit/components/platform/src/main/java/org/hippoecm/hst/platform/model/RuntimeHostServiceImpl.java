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
package org.hippoecm.hst.platform.model;

import java.util.UUID;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;

public class RuntimeHostServiceImpl implements RuntimeHostService {

    public void init() {
        HippoServiceRegistry.register(this, RuntimeHostService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, RuntimeHostService.class);
    }

    @Override
    public VirtualHosts create(final String hostName, final String sourceHostGroupName, final String autoHostTemplateURL, final String contextPath) {

        final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        final String targetHostGroupName = UUID.randomUUID().toString();

        HippoWebappContextRegistry.get().getEntries().forEach(hippoWebappContextServiceHolder -> {
            final String webappContextPath = hippoWebappContextServiceHolder.getServiceObject().getServletContext().getContextPath();
            final HstModelImpl hstModel = (HstModelImpl)hstModelRegistry.getHstModel(webappContextPath);

            hstModel.addRuntime(hostName, sourceHostGroupName, autoHostTemplateURL, targetHostGroupName);
        });

        return hstModelRegistry.getHstModel(contextPath).getVirtualHosts();
    }


}
