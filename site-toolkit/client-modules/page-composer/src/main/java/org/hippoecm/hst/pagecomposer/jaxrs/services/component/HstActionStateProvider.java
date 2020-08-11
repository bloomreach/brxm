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

import java.util.Optional;

final class HstActionStateProvider implements ActionStateProvider {

    private final HstActionProvider actionProvider;
    private final HstStateProvider stateProvider;

    HstActionStateProvider(final HstActionProvider actionProvider, final HstStateProvider stateProvider) {
        this.actionProvider = actionProvider;
        this.stateProvider = stateProvider;
    }

    @Override
    public Optional<ActionState> getActionState(final ActionStateProviderContext context) {
        return Optional.of(new ActionState(actionProvider.getActions(context), stateProvider.getStates(context)));
    }

}
