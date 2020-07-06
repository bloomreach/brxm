/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public final class ActionServiceImpl implements ActionService {

    private final ActionProviderContextFactory contextFactory;
    private List<ActionProvider> actionProviders;

    ActionServiceImpl(ActionProviderContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public void setActionProviders(final List<ActionProvider> actionProviders) {
        this.actionProviders = actionProviders;
    }

    @Override
    public Map<String, Set<Action>> getActionsByCategory(PageComposerContextService contextService, String siteMapItemUuid) throws RepositoryException {
        final ActionProviderContext context = contextFactory.make(contextService, siteMapItemUuid);
        return actionProviders.stream()
                .map(actionProvider -> Optional.ofNullable(actionProvider.getActions(context)).orElse(emptySet()))
                .flatMap(Set::stream)
                .collect(groupingBy(Action::getCategory, toSet()));
    }

}
