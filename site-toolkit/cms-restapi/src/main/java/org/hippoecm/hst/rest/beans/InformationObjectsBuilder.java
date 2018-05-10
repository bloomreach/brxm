/*
 *  Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.rest.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;

/**
 * Class to build different objects information to be easily serialized over the wire with different REST calls
 */
public final class InformationObjectsBuilder {

    @SuppressWarnings("unchecked")
    private static final Class<? extends ChannelInfo>[] EMPTY_CHANNEL_INFO_ARRAY = new Class[0];

    public static ChannelInfoClassInfo buildChannelInfoClassInfo(Class<? extends ChannelInfo> channelInfoClass) {
        return buildChannelInfoClassInfo(channelInfoClass, EMPTY_CHANNEL_INFO_ARRAY);
    }

    @SafeVarargs
    public static ChannelInfoClassInfo buildChannelInfoClassInfo(Class<? extends ChannelInfo> channelInfoClass,
            Class<? extends ChannelInfo>... channelInfoMixins) {
        final ChannelInfoClassInfo channelInfoClassInfo = new ChannelInfoClassInfo();
        channelInfoClassInfo.setClassName(channelInfoClass.getName());

        final List<FieldGroupInfo> fieldGroupListInfos = new LinkedList<>(
                buildFieldGroupListInfo(channelInfoClass.getAnnotation(FieldGroupList.class)));

        if (channelInfoMixins != null) {
            for (Class<? extends ChannelInfo> channelInfoMixin : channelInfoMixins) {
                fieldGroupListInfos.addAll(buildFieldGroupListInfo(channelInfoMixin.getAnnotation(FieldGroupList.class)));
            }
        }

        channelInfoClassInfo.setFieldGroups(fieldGroupListInfos);

        return channelInfoClassInfo;
    }

    public static List<FieldGroupInfo> buildFieldGroupListInfo(FieldGroupList fieldsGroup) {
        List<FieldGroupInfo> fieldsGroupList = new ArrayList<FieldGroupInfo>();

        if (fieldsGroup != null) {
            for (FieldGroup fieldGroup : fieldsGroup.value()) {
                try {
                    fieldsGroupList.add(buildFieldGroupInfo(fieldGroup));
                } catch (IllegalArgumentException iae) {
                }
            }
        }

        return fieldsGroupList; 
    }

    public static FieldGroupInfo buildFieldGroupInfo(FieldGroup fieldGroup) {
        if (fieldGroup == null) {
            throw new IllegalArgumentException("Got a 'null' field group argument!");
        }

        FieldGroupInfo fieldGroupInfo = new FieldGroupInfo();

        fieldGroupInfo.setValue(fieldGroup.value());
        fieldGroupInfo.setTitleKey(fieldGroup.titleKey());
        return fieldGroupInfo;
    }

    public static List<HstPropertyDefinitionInfo> buildHstPropertyDefinitionInfos(List<HstPropertyDefinition> hstPropertyDefinitions) {
        List<HstPropertyDefinitionInfo> hstPropertyDefinitionInfos = new ArrayList<HstPropertyDefinitionInfo>(hstPropertyDefinitions.size());

        for (HstPropertyDefinition hstPropertyDefinition : hstPropertyDefinitions) {
            hstPropertyDefinitionInfos.add(buildHstPropertyDefinitionInfo(hstPropertyDefinition));
        }

        return hstPropertyDefinitionInfos;
    }

    @SuppressWarnings("unchecked")
    public static HstPropertyDefinitionInfo buildHstPropertyDefinitionInfo(HstPropertyDefinition hstPropertyDefinition) {
        HstPropertyDefinitionInfo hstPropertyDefinitionInfo = new HstPropertyDefinitionInfo();

        hstPropertyDefinitionInfo.setIsRequired(hstPropertyDefinition.isRequired());
        hstPropertyDefinitionInfo.setDefaultValue(hstPropertyDefinition.getDefaultValue());
        hstPropertyDefinitionInfo.setName(hstPropertyDefinition.getName());
        hstPropertyDefinitionInfo.setValueType(hstPropertyDefinition.getValueType());
        hstPropertyDefinitionInfo.setAnnotations(hstPropertyDefinition.getAnnotations(Arrays.asList(ImageSetPath.class, JcrPath.class, DropDownList.class)));
        return hstPropertyDefinitionInfo;
    }

    public static Properties buildResourceBundleProperties(ResourceBundle resourceBundle) {
        Properties properties = new Properties();

        if (resourceBundle != null) {
            for (String key : resourceBundle.keySet()) {
                final String value = resourceBundle.getString(key);
                if (key == null || value == null) {
                    // Properties does not support null key or value
                    continue;
                }
                properties.put(key, value);
            }
        }

        return properties;
    }

}
