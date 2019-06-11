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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeIterator;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavigationItemServiceModuleTest {

    private NavigationItemResource resource;
    @Mock
    private SessionRequestContextProvider sessionRequestProvider;
    @Mock
    private HttpServletRequest request;

    @Mock
    private QueryManager queryManager;
    @Mock
    private Query query;
    @Mock
    private QueryResult queryResult;

    @Before
    public void setUp() throws RepositoryException {
        resource = (NavigationItemResource) new NavigationItemServiceModule().getRestResource(sessionRequestProvider);
    }

    @Test
    public void getNavigationItems() throws RepositoryException {

        final MockNode mockNode = MockNode.root(queryManager);

        final MockNode p1 = mockNode.addNode("p1", null);
        p1.setProperty("plugin.class", "x.y.z.A");

        final MockNode p2 = mockNode.addNode("p2", null);
        p2.setProperty("plugin.class", "x.y.z.B");

        // Without the plugin.class property
        final MockNode p3 = mockNode.addNode("p3", null);

        // with perspective-name resource
        final MockNode p4 = mockNode.addNode("p4", null);
        p4.setProperty("plugin.class", "org.hippoecm.frontend.navitems.DisplayNamePerspective");

        expect(queryManager.createQuery(anyString(), anyString())).andReturn(query);
        replay(queryManager);
        expect(query.execute()).andReturn(queryResult);
        replay(query);
        expect(queryResult.getNodes()).andReturn(new MockNodeIterator(Arrays.asList(p1, p2, p3, p4)));
        replay(queryResult);

        expect(sessionRequestProvider.getJcrSession(request)).andReturn(mockNode.getSession());
        expect(sessionRequestProvider.getFarthestRequestHost(request)).andReturn("cms.test.com");
        expect(sessionRequestProvider.getLocale(request)).andReturn(Locale.getDefault());
        replay(sessionRequestProvider);

        expect(request.getHeader("X-Forwarded-Proto")).andReturn("https");
        expect(request.getContextPath()).andReturn("/context-path");
        replay(request);

        final List<NavigationItem> navigationItems = resource.getNavigationItems(request);
        assertThat(navigationItems.size(), is(3));

        final NavigationItem item1 = navigationItems.get(0);
        assertThat(item1.getId(), is("hippo-perspective-a"));
        assertThat(item1.getAppIframeUrl(), is("https://cms.test.com/context-path/?iframe"));
        assertThat(item1.getAppPath(), is("hippo-perspective-a"));
        assertThat(item1.getDisplayName(), is(nullValue()));

        final NavigationItem item2 = navigationItems.get(1);
        assertThat(item2.getId(), is("hippo-perspective-b"));
        assertThat(item1.getAppIframeUrl(), is("https://cms.test.com/context-path/?iframe"));
        assertThat(item2.getAppPath(), is("hippo-perspective-b"));
        assertThat(item2.getDisplayName(), is(nullValue()));

        final NavigationItem item4 = navigationItems.get(2);
        assertThat(item4.getId(), is("hippo-perspective-displaynameperspective"));
        assertThat(item4.getAppIframeUrl(), is("https://cms.test.com/context-path/?iframe"));
        assertThat(item4.getAppPath(), is("hippo-perspective-displaynameperspective"));
        assertThat(item4.getDisplayName(), is("Dummy perspective"));
    }


    @Test
    public void getNavigationItems_EmptyList_On_RepositoryException() throws RepositoryException {

        final MockNode mockNode = MockNode.root(queryManager);

        expect(queryManager.createQuery(anyString(), anyString())).andThrow(new RepositoryException("Mock exception"));
        replay(queryManager);

        expect(sessionRequestProvider.getJcrSession(request)).andReturn(mockNode.getSession());
        expect(sessionRequestProvider.getFarthestRequestHost(request)).andReturn("cms.test.com");
        expect(sessionRequestProvider.getLocale(request)).andReturn(Locale.getDefault());
        replay(sessionRequestProvider);

        expect(request.getHeader("X-Forwarded-Proto")).andReturn("https");
        expect(request.getContextPath()).andReturn("/context-path");
        replay(request);

        final List<NavigationItem> navigationItems = resource.getNavigationItems(request);
        assertThat(navigationItems.isEmpty(), is(true));

    }
}
