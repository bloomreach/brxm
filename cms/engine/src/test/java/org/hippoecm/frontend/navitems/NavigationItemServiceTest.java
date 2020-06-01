/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.navitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.frontend.navigation.NavigationItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeIterator;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.frontend.navitems.NavigationItemStoreImpl.PLUGIN_PROPERTY_APP_PATH;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavigationItemServiceTest {

    private NavigationItemService service;

    @Mock
    private QueryManager queryManager;
    @Mock
    private Query query;
    @Mock
    private QueryResult queryResult;
    @Mock
    private LocalizationService localizationService;
    @Mock
    private ResourceBundle resourceBundle;

    @Before
    public void setUp() throws RepositoryException {
        HippoServiceRegistry.register(localizationService, LocalizationService.class);
        service = NavigationItemServiceModule.createNavigationItemService(node -> true);
    }

    @After
    public void tearDown() {
        HippoServiceRegistry.unregister(localizationService, LocalizationService.class);
    }

    @Test
    public void getNavigationItems() throws RepositoryException {

        final String appPath = PLUGIN_PROPERTY_APP_PATH;

        final MockNode mockNode = MockNode.root(queryManager);

        final int n = 4;
        int i = 0;
        final List<MockNode> navItemNodes = new ArrayList<>(n);
        for (; i < n; i++) {
            final MockNode app = mockNode.addNode("app-" + i, null);
            app.setProperty(appPath, "path-" + i);
            navItemNodes.add(app);
        }

        expect(queryManager.createQuery(anyString(), anyString())).andReturn(query);
        replay(queryManager);
        expect(query.execute()).andReturn(queryResult);
        replay(query);
        expect(queryResult.getNodes()).andReturn(new MockNodeIterator(navItemNodes));
        replay(queryResult);

        expect(localizationService.getResourceBundle(anyString(), anyObject())).andStubReturn(resourceBundle);
        replay(localizationService);

        i = 0;
        for (MockNode each : navItemNodes) {
            expect(resourceBundle.getString(each.getProperty(PLUGIN_PROPERTY_APP_PATH).getString())).andReturn("App " + i);
            i += 1;
        }
        replay(resourceBundle);

        final List<NavigationItem> navigationItems = service.getNavigationItems(mockNode.getSession(), Locale.getDefault());
        assertThat(navigationItems.size(), is(n));

        i = 0;
        for (NavigationItem item : navigationItems) {
            assertThat(item.getId(), is("xm-path-" + i));
            assertThat(item.getAppIframeUrl(), is(NavigationItemServiceImpl.APP_IFRAME_URL));
            assertThat(item.getAppPath(), is(navItemNodes.get(i).getProperty(appPath).getString()));
            assertThat(item.getDisplayName(), is("App " + i));
            i += 1;
        }
    }


    @Test
    public void getNavigationItems_EmptyList_On_RepositoryException() throws RepositoryException {

        final MockNode mockNode = MockNode.root(queryManager);
        expect(queryManager.createQuery(anyString(), anyString())).andThrow(new RepositoryException("Mock exception"));
        replay(queryManager);

        final List<NavigationItem> navigationItems = service.getNavigationItems(mockNode.getSession(), Locale.getDefault());
        assertThat(navigationItems.isEmpty(), is(true));

    }
}
