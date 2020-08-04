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
package org.hippoecm.hst.pagecomposer.jaxrs.services.state;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;

public class StateProviderContextFactory {

    private final XPageStateContextFactory xPageStateContextFactory;

    public StateProviderContextFactory(final XPageStateContextFactory xPageStateContextFactory) {
        this.xPageStateContextFactory = xPageStateContextFactory;
    }

    public StateProviderContext make(final StateContext stateContext) throws RepositoryException {
        final PageComposerContextService contextService = stateContext.getContextService();
        return new StateProviderContext()
                .setExperiencePageRequest(contextService.isExperiencePageRequest())
                .setXPageStateContext(xPageStateContextFactory.make(contextService));
    }

}
