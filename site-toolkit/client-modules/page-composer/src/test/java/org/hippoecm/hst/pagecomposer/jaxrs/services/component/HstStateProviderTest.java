/*
 * Copyright 2022 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class HstStateProviderTest {

    private HstStateProvider hstStateProvider = new HstStateProvider();

    @Test
    public void test_get_sorted_xpage_layouts() {

        final Set<XPageLayout> layouts = new LinkedHashSet<>();
        layouts.add(new XPageLayout("hst:xpages/aguide-format", "Guide", null));
        layouts.add(new XPageLayout("hst:xpages/complex-article", "Z-Complex", null));
        layouts.add(new XPageLayout("hst:xpages/standard-article", "Standard", null));
        layouts.add(new XPageLayout("hst:xpages/brand-page", "Brand", null));

        final ChannelContext channelContext = new ChannelContext();
        channelContext.setXPageLayouts(layouts);

        final ActionStateProviderContext context = new ActionStateProviderContext();
        context.setChannelContext(channelContext);

        final Map<NamedCategory, Object> states = hstStateProvider.getStates(context);

        final Map<String, String> expectedLayouts = new LinkedHashMap<>();
        expectedLayouts.put("hst:xpages/brand-page", "Brand");
        expectedLayouts.put("hst:xpages/aguide-format", "Guide");
        expectedLayouts.put("hst:xpages/standard-article", "Standard");
        expectedLayouts.put("hst:xpages/complex-article", "Z-Complex");

        final Object actualLayouts = states.get(HstState.CHANNEL_XPAGE_LAYOUTS);

        assertThat(actualLayouts).isInstanceOf(Map.class);
        assertThat(actualLayouts).isEqualTo(expectedLayouts);
    }

}