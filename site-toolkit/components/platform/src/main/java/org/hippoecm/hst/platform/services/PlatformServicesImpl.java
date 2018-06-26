/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services;

import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.platform.api.BlueprintService;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.DocumentService;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.onehippo.cms7.services.HippoServiceRegistry;

public class PlatformServicesImpl implements PlatformServices
{

    private HstModelRegistryImpl hstModelRegistry;
    private PreviewDecorator previewDecorator;

    private void init() {
        HippoServiceRegistry.registerService(this, PlatformServices.class);
    }

    private void stop() {
        HippoServiceRegistry.unregisterService(this, PlatformServices.class);
    }

    public void setPreviewDecorator(final PreviewDecorator previewDecorator) {
        this.previewDecorator = previewDecorator;
    }

    public void setHstModelRegistry(final HstModelRegistryImpl hstModelRegistry) {
        this.hstModelRegistry = hstModelRegistry;
    }

    @Override
    public BlueprintService getBlueprintService() {
        return new BlueprintServiceImpl(hstModelRegistry);
    }

    @Override
    public ChannelService getChannelService() {
        return new ChannelServiceImpl(hstModelRegistry, previewDecorator);
    }

    @Override
    public DocumentService getDocumentService() {
        return new DocumentServiceImpl(hstModelRegistry);
    }
}
