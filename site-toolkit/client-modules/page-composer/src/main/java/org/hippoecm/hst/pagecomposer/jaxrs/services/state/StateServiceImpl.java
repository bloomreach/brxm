/*
 *  Copyright 2020 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.state;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.RepositoryException;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public class StateServiceImpl implements StateService {

    private final StateProviderContextFactory contextFactory;
    private List<StateProvider> stateProviders;

    public StateServiceImpl(final StateProviderContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public void setStateProviders(final List<StateProvider> stateProviders) {
        this.stateProviders = stateProviders;
    }

    @Override
    public Map<String, Set<State>> getStatesByCategory(StateContext stateContext) throws RepositoryException {
        final StateProviderContext context = contextFactory.make(stateContext);
        if (!context.isExperiencePageRequest()) {
            return emptyMap();
        }
        return stateProviders.stream()
                .map(stateProvider -> Optional.ofNullable(stateProvider.getStates(context)).orElse(emptySet()))
                .flatMap(Set::stream)
                .collect(groupingBy(State::getCategory, toSet()));
    }
}
