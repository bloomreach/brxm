/*
 * Copyright 2019-2023 Bloomreach
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.frontend.navigation.NavigationItemService;
import org.jetbrains.annotations.NotNull;
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
import static org.hippoecm.frontend.navitems.NavigationItemStoreImpl.CMS_STATIC_PATH;
import static org.hippoecm.frontend.navitems.NavigationItemStoreImpl.NAVIGATIONITEM_MIXIN;
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

        final MockNode mockNode = MockNode.root(queryManager);
        // create all nodes from root node, so that session#getNode
        // does not throw an NoSuchElementException
        MockNode node = mockNode;
        for (String pathElement:CMS_STATIC_PATH.split("/")){
            if (!StringUtils.isEmpty(pathElement)){
                node = node.addNode(pathElement,null);
            }
        }
        final MockNode cmsStatic = node;
        // simulate the behaviour that NavigationItemStoreImpl
        // first gets the subnodes of #CMS_STATIC_PATH
        // and then the remaining items with a query
        final List<MockNode> orderedNavItems = createNavItemsByRange(cmsStatic, 0, 4);
        final List<MockNode> unorderedNavItems= createNavItemsByRange(cmsStatic, 4, 8);
        Collections.shuffle(unorderedNavItems);
        List<MockNode> navItems = new ArrayList<>(8);
        navItems.addAll(orderedNavItems);
        navItems.addAll(unorderedNavItems);

        expect(queryManager.createQuery(anyString(), anyString())).andReturn(query);
        replay(queryManager);
        expect(query.execute()).andReturn(queryResult);
        replay(query);
        expect(queryResult.getNodes()).andReturn(new MockNodeIterator(navItems));
        replay(queryResult);

        expect(localizationService.getResourceBundle(anyString(), anyObject())).andStubReturn(resourceBundle);
        replay(localizationService);

        int i = 0;
        for (MockNode each : navItems) {
            expect(resourceBundle.getString(each.getProperty(PLUGIN_PROPERTY_APP_PATH).getString())).andReturn("App " + i);
            i += 1;
        }
        replay(resourceBundle);

        final List<NavigationItem> navigationItems = service.getNavigationItems(mockNode.getSession(), Locale.getDefault());
        assertThat(navigationItems.size(), is(8));

        i = 0;
        for (NavigationItem item : navigationItems) {
            assertThat(item.getAppIframeUrl(), is(NavigationItemServiceImpl.APP_IFRAME_URL));
            if (i<4){
                assertThat(item.getAppPath(), is(orderedNavItems.get(i).getProperty(PLUGIN_PROPERTY_APP_PATH).getString()));
                assertThat(item.getId(), is("xm-path-" + i));
                assertThat(item.getDisplayName(), is("App " + i));
            }
            i += 1;
        }
    }

    @NotNull
    private List<MockNode> createNavItemsByRange(final MockNode cmsStatic, final int i2, final int i3) {
        return IntStream.range(i2, i3).mapToObj(i -> createPlugin(cmsStatic, i)).collect(Collectors.toList());
    }

    @NotNull
    private MockNode createPlugin(final MockNode cmsStatic, final int i){
        try {
            final MockNode app = cmsStatic.addNode("app-" + i, NAVIGATIONITEM_MIXIN);
            app.setProperty(PLUGIN_PROPERTY_APP_PATH, "path-" + i);
            return app;
        } catch (RepositoryException exception) {
            exception.printStackTrace();
        }
        return null;
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
