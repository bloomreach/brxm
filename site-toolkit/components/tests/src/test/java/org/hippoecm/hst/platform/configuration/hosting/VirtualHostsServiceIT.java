/*
 * Copyright 2020 Bloomreach
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

package org.hippoecm.hst.platform.configuration.hosting;

import java.util.Locale;
import java.util.ResourceBundle;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;

import static org.hamcrest.MatcherAssert.assertThat;

public class VirtualHostsServiceIT extends AbstractTestConfigurations {

    private HstManager hstSitesManager;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstSitesManager = getComponent(HstManager.class.getName());
    }

    @Test
    public void testChannelInfoResourceBundle() throws ContainerException {

        VirtualHosts virtualHosts = hstSitesManager.getVirtualHosts();

        Channel channel = virtualHosts.getChannels("testgroup").get("unittestsubproject");

        assertThat("The channel exists", channel, IsNull.notNullValue());
        ResourceBundle resourceBundle = virtualHosts.getResourceBundle(channel, Locale.ENGLISH);

        assertThat("The translation of the param for the channel info is resolved", resourceBundle.getString(
                "channelName"), IsEqual.equalTo("Channel Name"));
        assertThat("The translation of the inherited param for the channel info is resolved",
                resourceBundle.getString("baseParameter"), IsEqual.equalTo("Base Parameter"));
    }
}