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

final class ActionStateServiceImpl implements ActionStateService {

    private final ActionStateProviderContextFactory contextFactory;
    private List<ActionStateProvider> actionStateProviders;

    ActionStateServiceImpl(final ActionStateProviderContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public void setActionStateProviders(final List<ActionStateProvider> actionStateProviders) {
        this.actionStateProviders = actionStateProviders;
    }

    @Override
    public ActionState getActionState(ActionStateContext actionStateContext) throws RepositoryException {
        final ActionStateProviderContext context = contextFactory.make(actionStateContext);
        return actionStateProviders.stream()
                .map(actionStateProvider -> actionStateProvider
                        .getActionState(context)
                        .orElse(ActionState.empty()))
                .reduce(ActionState::merge)
                .orElse(ActionState.empty());
    }

}
