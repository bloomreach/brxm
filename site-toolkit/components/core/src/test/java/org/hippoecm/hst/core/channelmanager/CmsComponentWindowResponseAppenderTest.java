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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CmsComponentWindowResponseAppenderTest {

    private CmsComponentWindowResponseAppender appender;

    @Before
    public void setUp() {
        appender = new CmsComponentWindowResponseAppender();
    }

    @Test
    public void testGetAttributeMap_without_attribute_contributors() {
        assertThat(appender.getAttributeMap(null, null).size(), is(0));
    }

    @Test
    public void testGetAttributeMap_with_one_contributor() {

        final AttributeContributor contributor = createNiceMock(AttributeContributor.class);
        final Map<String, String> map = new HashMap<>();
        expect(contributor.contribute(eq(null), eq(null), isA(Map.class))).andReturn(map).once();
        replay(contributor);

        appender.setAttributeContributors(Arrays.asList(contributor));
        assertThat(appender.getAttributeMap(null, null), is(map));
    }

    @Test
    public void testGetAttributeMap_with_three_contributors() {

        final AttributeContributor contributor = createNiceMock(AttributeContributor.class);
        appender.setAttributeContributors(Arrays.asList(contributor, contributor, contributor));

        final Map<String, String> m1 = new HashMap<>(), m2 = new HashMap<>(), m3 = new HashMap<>();
        expect(contributor.contribute(eq(null), eq(null), isA(Map.class))).andReturn(m1);
        expect(contributor.contribute(null, null, m1)).andReturn(m2);
        expect(contributor.contribute(null, null, m2)).andReturn(m3);
        replay(contributor);

        m3.put("foo", "bar");
        assertThat(appender.getAttributeMap(null, null).get("foo"), is(equalTo("bar")));
    }

}
