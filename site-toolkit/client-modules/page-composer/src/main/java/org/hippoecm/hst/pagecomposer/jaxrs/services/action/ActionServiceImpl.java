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
import java.util.Set;
import java.util.function.Function;

import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public final class ActionServiceImpl implements ActionService {

    private final Function<PageComposerContextService, ActionProviderContext> contextProvider;
    private List<ActionProvider> actionProviders;

    ActionServiceImpl(Function<PageComposerContextService, ActionProviderContext> contextProvider) {
        this.contextProvider = contextProvider;
    }

    public ActionServiceImpl() {
        this(ActionProviderContextImpl::new);
    }

    public void setActionProviders(final List<ActionProvider> actionProviders) {
        this.actionProviders = actionProviders;
    }

    @Override
    public Map<String, Set<Action>> getActionsByCategory(final PageComposerContextService contextService) {
        final ActionProviderContext context = contextProvider.apply(contextService);
        return actionProviders.stream()
                .map(actionProvider -> actionProvider.getActions(context))
                .flatMap(Set::stream)
                .collect(groupingBy(Action::getCategory, toSet()));
    }

}
