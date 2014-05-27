/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.configuration.channel;

import java.util.Map;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.MutableHstManager;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ChannelManagerOtherContextPath extends AbstractTestConfigurations {

    private HstManager hstManager;
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final MockHstRequestContext requestContext = new MockHstRequestContext();
        requestContext.setAttribute("HOST_GROUP_NAME_FOR_CMS_HOST", "dev-localhost");
        //ModifiableRequestContextProvider.set(requestContext);
        hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void channels_for_current_contextpath_slashsite2_only_are_loaded() throws Exception {
        // now change the contextpath to site2: Now only 'intranettestchannel' is expected to be part of the channels

        ((MutableHstManager)componentManager.getComponent(HstManager.class.getName()))
                .setContextPath("/site2");
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        assertFalse("testchannel should not be part of channels since has wrong contextpath",
                channels.containsKey("testchannel"));
        assertTrue("intranettestchannel should be part of channels since has wrong contextpath",
                channels.containsKey("intranettestchannel"));
    }
}
