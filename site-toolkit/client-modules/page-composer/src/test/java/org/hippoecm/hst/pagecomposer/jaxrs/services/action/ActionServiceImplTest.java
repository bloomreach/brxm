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

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.jcr.RepositoryException;

import org.assertj.core.api.Assertions;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.hst.Channel;

import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.channel;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.page;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.action.HstCategories.xpage;

@RunWith(EasyMockRunner.class)
public class ActionServiceImplTest {

    private ActionServiceImpl service;
    @Mock
    private PageComposerContextService contextService;
    @Mock
    Function<PageComposerContextService, ActionProviderContext> contextProvider;
    @Mock
    private ActionProviderContext context;
    @Mock
    private Channel channel;

    @Before
    public void setUp() throws RepositoryException {
        service = new ActionServiceImpl(contextProvider);
        service.setActionProviders(singletonList(new HstActionProviderImpl()));

        expect(contextProvider.apply(contextService)).andStubReturn(context);
        replay(contextProvider);
        expect(context.getContextService()).andStubReturn(contextService);
        replay(context);
    }

    @Test
    public void get_menu() {

        expect(contextService.getEditingPreviewChannel()).andStubReturn(channel);
        expect(contextService.isExperiencePageRequest()).andReturn(true);
        replay(contextService);

        final Map<String, Set<Action>> actions = service.getActionsByCategory(contextService);

        Assertions.assertThat(actions.get(channel().getName())).isNotEmpty();
        Assertions.assertThat(actions.get(page().getName())).isNotEmpty();
        Assertions.assertThat(actions.get(xpage().getName())).isNotEmpty();
    }


    @Test
    public void get_menu_no_xpage() {

        expect(contextService.getEditingPreviewChannel()).andStubReturn(channel);
        expect(contextService.isExperiencePageRequest()).andReturn(false);
        replay(contextService);

        final Map<String, Set<Action>> actions = service.getActionsByCategory(contextService);

        Assertions.assertThat(actions.get(channel().getName())).isNotEmpty();
        Assertions.assertThat(actions.get(page().getName())).isNotEmpty();
        Assertions.assertThat(actions.get(xpage().getName())).isNull();
    }

    @Test
    public void get_menu_page_disabled() {

        expect(channel.isConfigurationLocked()).andReturn(true);
        replay(channel);
        expect(contextService.getEditingPreviewChannel()).andStubReturn(channel);
        expect(contextService.isExperiencePageRequest()).andReturn(false);
        replay(contextService);

        final Map<String, Set<Action>> actions = service.getActionsByCategory(contextService);

        Assertions.assertThat(actions.get(channel().getName())).isNotEmpty();
        Assertions.assertThat(actions.get(page().getName())).isNull();
        Assertions.assertThat(actions.get(xpage().getName())).isNull();
    }
}
