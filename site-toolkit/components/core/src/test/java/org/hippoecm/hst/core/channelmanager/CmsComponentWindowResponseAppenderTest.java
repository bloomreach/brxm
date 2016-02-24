/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CmsComponentWindowResponseAppenderTest {

    private CmsComponentWindowResponseAppender appender;

    @Before
    public void setUp() {
        appender = new CmsComponentWindowResponseAppender();
    }

    @Test
    public void testGetAttributeMap_without_attribute_contributors() {
        final Map<String, String> preambleAttributeMap = new HashMap<>();
        final Map<String, String> epilogueAttributeMap = new HashMap<>();

        appender.populateAttributes(null, null, preambleAttributeMap, epilogueAttributeMap);

        assertThat(preambleAttributeMap.size(), is(0));
        assertThat(epilogueAttributeMap.size(), is(0));
    }

    @Test
    public void testGetAttributeMap_with_one_contributor() {

        final ComponentWindowAttributeContributor contributor = new ComponentWindowAttributeContributor() {
            @Override
            public void contributePreamble(final HstComponentWindow window, final HstRequest request, final Map<String, String> populatingAttributesMap) {
                populatingAttributesMap.put("foo","bar");
            }
            @Override
            public void contributeEpilogue(final HstComponentWindow window, final HstRequest request, final Map<String, String> populatingAttributesMap) {
                populatingAttributesMap.put("bar","foo");
            }
        };

        final List<ComponentWindowAttributeContributor> contributorList = new ArrayList<>();
        contributorList.add(contributor);
        appender.setAttributeContributors(contributorList);

        final Map<String, String> preambleAttributeMap = new HashMap<>();
        final Map<String, String> epilogueAttributeMap = new HashMap<>();

        appender.populateAttributes(null, null, preambleAttributeMap, epilogueAttributeMap);
        assertEquals("bar", preambleAttributeMap.get("foo"));
        assertEquals("foo", epilogueAttributeMap.get("bar"));
    }

    @Test
    public void testGetAttributeMap_with_two_contributors() {

        final ComponentWindowAttributeContributor contributor1 = new ComponentWindowAttributeContributor() {
            @Override
            public void contributePreamble(final HstComponentWindow window, final HstRequest request, final Map<String, String> populatingAttributesMap) {
                populatingAttributesMap.put("foo1","bar1");
            }
            @Override
            public void contributeEpilogue(final HstComponentWindow window, final HstRequest request, final Map<String, String> populatingAttributesMap) {
                populatingAttributesMap.put("bar1","foo1");
            }
        };

        final ComponentWindowAttributeContributor contributor2 = new ComponentWindowAttributeContributor() {
            @Override
            public void contributePreamble(final HstComponentWindow window, final HstRequest request, final Map<String, String> populatingAttributesMap) {
                populatingAttributesMap.put("foo2","bar2");
            }
            @Override
            public void contributeEpilogue(final HstComponentWindow window, final HstRequest request, final Map<String, String> populatingAttributesMap) {
                populatingAttributesMap.put("bar2","foo2");
            }
        };

        final List<ComponentWindowAttributeContributor> contributorList = new ArrayList<>();
        contributorList.add(contributor1);
        contributorList.add(contributor2);

        appender.setAttributeContributors(contributorList);

        final Map<String, String> preambleAttributeMap = new HashMap<>();
        final Map<String, String> epilogueAttributeMap = new HashMap<>();

        appender.populateAttributes(null, null, preambleAttributeMap, epilogueAttributeMap);
        assertEquals("bar1", preambleAttributeMap.get("foo1"));
        assertEquals("bar2", preambleAttributeMap.get("foo2"));
        assertEquals("foo1", epilogueAttributeMap.get("bar1"));
        assertEquals("foo2", epilogueAttributeMap.get("bar2"));
    }

}
