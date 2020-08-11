/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import java.util.List;

import javax.jcr.RepositoryException;

final class ComponentInfoServiceImpl implements ComponentInfoService {

    private final ComponentInfoProviderContextFactory contextFactory;
    private List<ComponentInfoProvider> componentInfoProviders;

    ComponentInfoServiceImpl(final ComponentInfoProviderContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public void setComponentInfoProviders(final List<ComponentInfoProvider> componentInfoProviders) {
        this.componentInfoProviders = componentInfoProviders;
    }

    @Override
    public ComponentInfo getComponentInfo(ComponentInfoContext componentInfoContext) throws RepositoryException {
        final ComponentInfoProviderContext context = contextFactory.make(componentInfoContext);
        return componentInfoProviders.stream()
                .map(componentInfoProvider -> componentInfoProvider
                        .getComponentInfo(context)
                        .orElse(ComponentInfo.empty()))
                .reduce(ComponentInfo::merge)
                .orElse(ComponentInfo.empty());
    }

}
