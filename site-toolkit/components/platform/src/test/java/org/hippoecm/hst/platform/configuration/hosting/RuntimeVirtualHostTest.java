/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Collections;

import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.platform.configuration.RuntimeHstSiteMenuItemConfiguration;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RuntimeVirtualHostTest {

    @Test
    public void testSiteMenuItemConfiguration() {
        final HstSiteMenuItemConfiguration mockMenuItemConfiguration = createMock(HstSiteMenuItemConfiguration.class);
        final HstSiteMenuItemConfiguration mockChildMenuItemConfiguration = createMock(HstSiteMenuItemConfiguration.class);

        expect(mockChildMenuItemConfiguration.getChildItemConfigurations()).andReturn(Collections.emptyList());
        replay(mockChildMenuItemConfiguration);

        expect(mockMenuItemConfiguration.getChildItemConfigurations()).andReturn(Arrays.asList(mockChildMenuItemConfiguration));
        replay(mockMenuItemConfiguration);

        final RuntimeHstSiteMenuItemConfiguration menuItemConfiguration = new RuntimeHstSiteMenuItemConfiguration(
                mockMenuItemConfiguration, null, null);

        final HstSiteMenuItemConfiguration childMenuItemConfiguration = menuItemConfiguration.getChildItemConfigurations().get(0);

        assertThat(menuItemConfiguration.getParentItemConfiguration(), nullValue());
        assertEquals(childMenuItemConfiguration.getParentItemConfiguration(), menuItemConfiguration);
        assertEquals(childMenuItemConfiguration.getChildItemConfigurations().size(), 0);
    }

}
