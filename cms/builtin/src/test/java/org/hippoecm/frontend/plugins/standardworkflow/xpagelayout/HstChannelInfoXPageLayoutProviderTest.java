/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standardworkflow.xpagelayout;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.onehippo.cms7.services.hst.IXPageLayout;
import org.onehippo.cms7.services.hst.XPageLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class HstChannelInfoXPageLayoutProviderTest {

    private static final Logger log = LoggerFactory.getLogger(HstChannelInfoXPageLayoutProviderTest.class);

    @Test
    public void getXPageLayouts() {
        List<IXPageLayout> expected = Stream.of(
                new XPageLayout("layout1","Layout 1","uuid1"),
                new XPageLayout("layout2", "Layout 2", "uuid2")
        ).collect(Collectors.toList());

        final List<JSONObject> jsonObjects = expected.stream().map(l -> new JSONObject(l)).collect(Collectors.toList());
        PlainJcrHstChannelInfoXPageLayoutProvider layoutProvider = new PlainJcrHstChannelInfoXPageLayoutProvider("anyId");
        final String xPageLayouts = new JSONArray(jsonObjects).toString();
        final List<IXPageLayout> actual = layoutProvider.parseXPageLayoutsJSONString(xPageLayouts);
        assertEquals(expected, actual);
    }
}
