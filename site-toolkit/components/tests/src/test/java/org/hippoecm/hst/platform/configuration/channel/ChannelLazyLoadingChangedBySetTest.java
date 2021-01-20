/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.platform.configuration.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.mock.configuration.MockSiteMapItem;
import org.hippoecm.hst.mock.configuration.MockSiteMenuConfiguration;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.platform.configuration.channel.ChannelLazyLoadingChangedBySet;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChannelLazyLoadingChangedBySetTest {

    private ChannelLazyLoadingChangedBySet set;

    private HstNode rootConfigNode;
    private HstSite previewSite;
    private Channel channel;
    private HstComponentsConfiguration componentsConfig;
    private MockHstComponentConfiguration componentConfig;
    private HstSiteMap siteMap;
    private MockSiteMapItem siteMapItem;
    private HstSiteMenusConfiguration menusConfig;
    private MockSiteMenuConfiguration menuConfig;
    private Object[] mocks;

    @Before
    public void setUp() {

        rootConfigNode = createMock(HstNode.class);
        previewSite = createMock(HstSite.class);
        channel = createMock(Channel.class);

        componentsConfig = createMock(HstComponentsConfiguration.class);
        componentConfig = createMock(MockHstComponentConfiguration.class);
        siteMap = createMock(HstSiteMap.class);
        siteMapItem = createMock(MockSiteMapItem.class);

        menusConfig = createMock(HstSiteMenusConfiguration.class);
        menuConfig = createMock(MockSiteMenuConfiguration.class);

        mocks = new Object[]{rootConfigNode, previewSite, channel, componentsConfig, componentConfig,
                siteMap, siteMapItem, menusConfig, menuConfig};
        reset(mocks);
    }

    @Test
    public void test_unlocked() {

        expect(previewSite.getSiteMap()).andReturn(siteMap);
        final List<HstSiteMapItem> siteMapItems = Collections.emptyList();
        expect(siteMap.getSiteMapItems()).andReturn(siteMapItems);

        expect(previewSite.getComponentsConfiguration()).andReturn(componentsConfig);
        final Map<String, HstComponentConfiguration> configurationMap = Collections.emptyMap();
        expect(componentsConfig.getComponentConfigurations()).andReturn(configurationMap);

        expect(previewSite.getSiteMenusConfiguration()).andReturn(menusConfig);
        final Map<String, HstSiteMenuConfiguration> menuMap = Collections.emptyMap();
        expect(menusConfig.getSiteMenuConfigurations()).andReturn(menuMap);

        final List<HstNode> childNodes = Collections.emptyList();
        expect(rootConfigNode.getNodes()).andReturn(childNodes);

        expect(channel.getChannelNodeLockedBy()).andReturn(null);
        replay(mocks);

        set = new ChannelLazyLoadingChangedBySet(previewSite, channel, null);
        assertThat(set.isEmpty(), is(true));
    }


    @Test
    public void test_locked_on_menu_level() {

        expect(previewSite.getSiteMap()).andReturn(siteMap);
        final List<HstSiteMapItem> siteMapItems = Collections.emptyList();
        expect(siteMap.getSiteMapItems()).andReturn(siteMapItems);

        expect(previewSite.getComponentsConfiguration()).andReturn(componentsConfig);
        expect(previewSite.getConfigurationPath()).andReturn("/hst:hst/hst:configurations/myproject");
        final Map<String, HstComponentConfiguration> configurationMap = Collections.emptyMap();
        expect(componentsConfig.getComponentConfigurations()).andReturn(configurationMap);

        expect(previewSite.getSiteMenusConfiguration()).andReturn(menusConfig);
        final Map<String, HstSiteMenuConfiguration> menuMap = new HashMap<>();

        expect(menuConfig.getCanonicalPath()).andReturn("/hst:hst/hst:configurations/myproject/hst:sitemenus/home").anyTimes();
        expect(menuConfig.getLockedBy()).andReturn("joe").atLeastOnce();
        menuMap.put("menu-1", menuConfig);
        expect(menusConfig.getSiteMenuConfigurations()).andReturn(menuMap);

        final List<HstNode> childNodes = Collections.emptyList();
        expect(rootConfigNode.getNodes()).andReturn(childNodes);

        expect(channel.getChannelNodeLockedBy()).andReturn(null);
        replay(mocks);
        set = new ChannelLazyLoadingChangedBySet(previewSite, channel, null);
        assertThat(set.contains("joe"), is(true));

    }

    @Test
    public void test_locked_on_component_level() {

        expect(previewSite.getSiteMap()).andReturn(siteMap);
        final List<HstSiteMapItem> siteMapItems = Collections.emptyList();
        expect(siteMap.getSiteMapItems()).andReturn(siteMapItems);

        expect(previewSite.getComponentsConfiguration()).andReturn(componentsConfig);
        final Map<String, HstComponentConfiguration> configurationMap = new HashMap<>();
        expect(componentConfig.getLockedBy()).andReturn("john").atLeastOnce();
        expect(componentConfig.isInherited()).andReturn(false);

        final SortedMap<String, HstComponentConfiguration> empty = new TreeMap<>();
        expect(componentConfig.getChildren()).andReturn(empty);
        configurationMap.put("config-1", componentConfig);
        expect(componentsConfig.getComponentConfigurations()).andReturn(configurationMap);

        expect(previewSite.getSiteMenusConfiguration()).andReturn(menusConfig);
        final Map<String, HstSiteMenuConfiguration> menuMap = Collections.emptyMap();
        expect(menusConfig.getSiteMenuConfigurations()).andReturn(menuMap);

        final List<HstNode> childNodes = Collections.emptyList();
        expect(rootConfigNode.getNodes()).andReturn(childNodes);

        expect(channel.getChannelNodeLockedBy()).andReturn(null);
        replay(mocks);

        set = new ChannelLazyLoadingChangedBySet(previewSite, channel, null);
        assertThat(set.contains("john"), is(true));

    }

    @Test
    public void test_locked_on_channel_level() {

        expect(previewSite.getSiteMap()).andReturn(siteMap);
        final List<HstSiteMapItem> siteMapItems = Collections.emptyList();
        expect(siteMap.getSiteMapItems()).andReturn(siteMapItems);
        expect(previewSite.getComponentsConfiguration()).andReturn(componentsConfig);
        final Map<String, HstComponentConfiguration> configurationMap = Collections.emptyMap();
        expect(componentsConfig.getComponentConfigurations()).andReturn(configurationMap);

        expect(previewSite.getSiteMenusConfiguration()).andReturn(menusConfig);
        final Map<String, HstSiteMenuConfiguration> menuMap = Collections.emptyMap();
        expect(menusConfig.getSiteMenuConfigurations()).andReturn(menuMap);

        final List<HstNode> childNodes = Collections.emptyList();
        expect(rootConfigNode.getNodes()).andReturn(childNodes);

        expect(channel.getChannelNodeLockedBy()).andReturn("john").atLeastOnce();
        replay(mocks);

        set = new ChannelLazyLoadingChangedBySet(previewSite, channel, null);
        assertThat(set.contains("john"), is(true));

    }


    @Test
    public void test_locked_on_sitemap_level() {

        expect(previewSite.getConfigurationPath()).andReturn("/hst:hst/hst:configurations/myproject").anyTimes();

        expect(previewSite.getSiteMap()).andReturn(siteMap);
        final List<HstSiteMapItem> siteMapItems = new ArrayList<>();
        siteMapItems.add(siteMapItem);
        expect(siteMapItem.getCanonicalPath()).andReturn("/hst:hst/hst:configurations/myproject/hst:sitemap/home").anyTimes();
        expect(siteMapItem.getLockedBy()).andReturn("john").atLeastOnce();
        List<HstSiteMapItem> empty = Collections.emptyList();
        expect(siteMapItem.getChildren()).andReturn(empty).anyTimes();

        expect(siteMap.getSiteMapItems()).andReturn(siteMapItems);

        expect(previewSite.getComponentsConfiguration()).andReturn(componentsConfig);
        final Map<String, HstComponentConfiguration> configurationMap = Collections.emptyMap();
        expect(componentsConfig.getComponentConfigurations()).andReturn(configurationMap);

        expect(previewSite.getSiteMenusConfiguration()).andReturn(menusConfig);
        final Map<String, HstSiteMenuConfiguration> menuMap = Collections.emptyMap();
        expect(menusConfig.getSiteMenuConfigurations()).andReturn(menuMap);

        final List<HstNode> childNodes = Collections.emptyList();
        expect(rootConfigNode.getNodes()).andReturn(childNodes);

        expect(channel.getChannelNodeLockedBy()).andReturn(null);

        replay(mocks);

        set = new ChannelLazyLoadingChangedBySet(previewSite, channel, null);
        assertThat(set.contains("john"), is(true));
    }

}
