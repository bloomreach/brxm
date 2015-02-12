/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
 */

package org.hippoecm.hst.core.channelmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type.CONTAINER_COMPONENT;
import static org.junit.Assert.assertThat;

public class CmsComponentComponentWindowAttributeContributorTest {

    private List<Object> mocks;
    private HstComponentWindow window;
    private HstRequest request;

    private CmsComponentComponentWindowAttributeContributor contributor;

    @Before
    public void setUp() {
        mocks = new ArrayList<>();
        window = mock(HstComponentWindow.class);
        request = mock(HstRequest.class);

        contributor = new CmsComponentComponentWindowAttributeContributor();
    }

    @Test
    public void testContribute() {

        final HstComponentConfiguration config = mock(HstComponentConfiguration.class);
        expect(window.getComponentInfo()).andReturn(config);
        expect(window.getReferenceNamespace()).andReturn("reference-namespace");
        expect(config.getComponentType()).andReturn(CONTAINER_COMPONENT);
        final HstRequestContext context = mock(HstRequestContext.class);
        expect(request.getRequestContext()).andReturn(context);
        final HstURLFactory urlFactory = mock(HstURLFactory.class);
        expect(context.getURLFactory()).andReturn(urlFactory);
        final HstURL url = mock(HstURL.class);
        expect(urlFactory.createURL(isA(String.class), isA(String.class), eq(null), eq(context))).andReturn(url);
        replay(mocks.toArray());

        final Map<String, String> map = new HashMap<>();
        contributor.contribute(window, request, map);

        assertThat(map.containsKey("xtype"), is(false));
        assertThat(map.containsKey("inherited"), is(false));

        assertThat(map.containsKey("uuid"), is(true));
        assertThat(map.containsKey("type"), is(true));
        assertThat(map.containsKey("refNS"), is(true));
        assertThat(map.containsKey("url"), is(true));
    }

    private <T> T mock(final Class<T> type) {
        final T mock = createNiceMock(type);
        mocks.add(mock);
        return mock;
    }
}
