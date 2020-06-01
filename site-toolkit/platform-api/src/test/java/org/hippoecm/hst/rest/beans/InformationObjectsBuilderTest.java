/**
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.rest.beans;

import java.util.List;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.platform.api.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.platform.api.beans.FieldGroupInfo;
import org.hippoecm.hst.platform.api.beans.InformationObjectsBuilder;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InformationObjectsBuilderTest {

    static interface TestInfoWithoutAnnotation extends ChannelInfo {
        @Parameter(name = "test-name")
        String getName();
    }

    @FieldGroupList({
        @FieldGroup(
                titleKey = "fields.test",
                value = { "test-name" }
        )
    })
    static interface TestInfo extends ChannelInfo {
        @Parameter(name = "test-name")
        String getName();
    }

    @FieldGroupList({
        @FieldGroup(
                titleKey = "fields.analytics",
                value = { "analyticsEnabled", "scriptlet" }
        )
    })
    static interface AnalyticsChannelInfoMixin extends ChannelInfo {

        @Parameter(name = "analyticsEnabled")
        Boolean isAnalyticsEnabled();

        @Parameter(name = "scriptlet")
        String getScriptlet();

    }

    @FieldGroupList({
        @FieldGroup(
                titleKey = "fields.categorization",
                value = { "categorizationEnabled", "categories" }
        )
    })
    static interface CategorizingChannelInfoMixin extends ChannelInfo {

        @Parameter(name = "categorizationEnabled")
        Boolean isCategorizationEnabled();

        @Parameter(name = "categories")
        String getCategories();

    }

    @Test
    public void testBuildChannelInfoClassInfoHavingNoAnnotation() throws Exception {
        ChannelInfoClassInfo channelInfoClassInfo = InformationObjectsBuilder
                .buildChannelInfoClassInfo(TestInfoWithoutAnnotation.class);

        assertEquals(TestInfoWithoutAnnotation.class.getName(), channelInfoClassInfo.getClassName());

        List<FieldGroupInfo> fieldGroupInfos = channelInfoClassInfo.getFieldGroups();

        assertTrue(fieldGroupInfos.isEmpty());
    }

    @Test
    public void testBuildChannelInfoClassInfo() throws Exception {
        ChannelInfoClassInfo channelInfoClassInfo = InformationObjectsBuilder
                .buildChannelInfoClassInfo(TestInfo.class);

        assertEquals(TestInfo.class.getName(), channelInfoClassInfo.getClassName());

        List<FieldGroupInfo> fieldGroupInfos = channelInfoClassInfo.getFieldGroups();

        assertEquals(1, fieldGroupInfos.size());

        FieldGroupInfo fieldGroupInfo = fieldGroupInfos.get(0);

        assertEquals("fields.test", fieldGroupInfo.getTitleKey());
        assertArrayEquals(new String[] { "test-name" }, fieldGroupInfo.getValue());
    }

    @Test
    public void testBuildChannelInfoClassInfoWithMixins() throws Exception {
        ChannelInfoClassInfo channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(TestInfo.class,
                AnalyticsChannelInfoMixin.class, CategorizingChannelInfoMixin.class);

        assertEquals(TestInfo.class.getName(), channelInfoClassInfo.getClassName());

        List<FieldGroupInfo> fieldGroupInfos = channelInfoClassInfo.getFieldGroups();

        assertEquals(3, fieldGroupInfos.size());

        FieldGroupInfo fieldGroupInfo = fieldGroupInfos.get(0);

        assertEquals("fields.test", fieldGroupInfo.getTitleKey());
        assertArrayEquals(new String[] { "test-name" }, fieldGroupInfo.getValue());

        fieldGroupInfo = fieldGroupInfos.get(1);

        assertEquals("fields.analytics", fieldGroupInfo.getTitleKey());
        assertArrayEquals(new String[] { "analyticsEnabled", "scriptlet" }, fieldGroupInfo.getValue());

        fieldGroupInfo = fieldGroupInfos.get(2);

        assertEquals("fields.categorization", fieldGroupInfo.getTitleKey());
        assertArrayEquals(new String[] { "categorizationEnabled", "categories" }, fieldGroupInfo.getValue());
    }
}
