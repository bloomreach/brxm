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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.jcr.RepositoryException;

import org.assertj.core.api.Assertions;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

@RunWith(EasyMockRunner.class)
public class ActionServiceImplTest {

    private ActionServiceImpl service;
    @Mock
    private ActionContext actionContext;
    @Mock
    private ActionProviderContextFactory contextFactory;
    @Mock
    private ActionProvider actionProvider;

    @Before
    public void setUp() throws RepositoryException {
        service = new ActionServiceImpl(contextFactory);
        service.setActionProviders(singletonList(actionProvider));
    }

    @Test
    public void get_actions_empty() throws RepositoryException {
        final Map<String, Set<Action>> actions = service.getActionsByCategory(actionContext);
        Assertions.assertThat(actions).isEmpty();
    }

    @Test
    public void get_actions_grouped_by_category() throws RepositoryException {

        final int nrOfCategories = ThreadLocalRandom.current().nextInt(2, 32);
        final int nrOfActions = ThreadLocalRandom.current().nextInt(2, 32);
        final Set<Action> actionSet = IntStream.range(0, nrOfCategories)
                .mapToObj(i -> "c-" + i)
                .flatMap(category -> Collections.nCopies(nrOfActions, category).stream())
                .map(category -> new Action(UUID.randomUUID().toString(), category, true))
                .collect(Collectors.toSet());

        expect(actionProvider.getActions(null))
                .andReturn(actionSet);
        replay(actionProvider);

        final Map<String, Set<Action>> actionsByCategory = service.getActionsByCategory(actionContext);
        Assertions.assertThat(actionsByCategory.size()).isEqualTo(nrOfCategories);
        actionsByCategory.values().forEach(
                actions -> Assertions.assertThat(actions.size()).isEqualTo(nrOfActions));
    }

}
